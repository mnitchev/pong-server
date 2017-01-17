package bg.uni_sofia.s81167.pong.main;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import bg.uni_sofia.s81167.pong.game.GameContext;
import bg.uni_sofia.s81167.pong.jobs.ConnectionListenerJob;;

public class Main {
	public static void main(String[] args) throws SchedulerException{
		StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
		Scheduler scheduler = schedulerFactory.getScheduler();
		
		scheduler.start();
		scheduleInitialJobs(scheduler);
	}

	private static void scheduleInitialJobs(Scheduler scheduler) throws SchedulerException {
		ConcurrentHashMap<String, GameContext> games = new ConcurrentHashMap<>();
		ConcurrentHashMap<String, Socket> activeUsers = new ConcurrentHashMap<>();
		JobDataMap listenerMap = createListenerMap(games, activeUsers);
		scheduleJob(scheduler, ConnectionListenerJob.class, listenerMap, "connectionListener");
	}

	private static JobDataMap createListenerMap(ConcurrentHashMap<String, GameContext> games,
			ConcurrentHashMap<String, Socket> activeUsers) {
		JobDataMap listenerMap = new JobDataMap();
		listenerMap.put("activeGames", games);
		listenerMap.put("activeUsers", activeUsers);
		return listenerMap;
	}

	private static void scheduleJob(Scheduler scheduler, Class<? extends Job> jobClass, JobDataMap map, String jobId) throws SchedulerException {
		JobDetail connectionListenerJob = newJob(jobClass)
				.withIdentity(jobId, "init")
				.setJobData(map)
				.build();
		
		Trigger connectionListenerTrigger = newTrigger()
				.withIdentity(jobId, "init")
				.startNow()
				.build();
		
		scheduler.scheduleJob(connectionListenerJob, connectionListenerTrigger);
	}
}

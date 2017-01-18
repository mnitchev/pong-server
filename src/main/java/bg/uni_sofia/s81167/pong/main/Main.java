package bg.uni_sofia.s81167.pong.main;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import bg.uni_sofia.s81167.dao.DataSourceUserDAO;
import bg.uni_sofia.s81167.pong.jobs.ConnectionListenerJob;;

public class Main {
	public static void main(String[] args) throws SchedulerException {
		StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
		Scheduler scheduler = schedulerFactory.getScheduler();

		scheduler.start();
		scheduleInitialJobs(scheduler);
	}

	private static void scheduleInitialJobs(Scheduler scheduler) throws SchedulerException {
		JobDataMap listenerMap;
		listenerMap = createListenerMap();
		scheduleJob(scheduler, ConnectionListenerJob.class, listenerMap, "connectionListener");
	}

	private static JobDataMap createListenerMap() {
		JobDataMap listenerMap = new JobDataMap();
		listenerMap.put("activeGames", new ConcurrentHashMap<>());
		Set<String> activeUsers = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		listenerMap.put("activeUsers", activeUsers);
		try {
			listenerMap.put("userDAO", new DataSourceUserDAO());
		} catch (NamingException | SQLException e) {
			throw new RuntimeException(e);
		}
		return listenerMap;
	}

	private static void scheduleJob(Scheduler scheduler, Class<? extends Job> jobClass, JobDataMap map, String jobId)
			throws SchedulerException {
		JobDetail connectionListenerJob = newJob(jobClass).withIdentity(jobId, "init").setJobData(map).build();

		Trigger connectionListenerTrigger = newTrigger().withIdentity(jobId, "init").startNow().build();

		scheduler.scheduleJob(connectionListenerJob, connectionListenerTrigger);
	}
}

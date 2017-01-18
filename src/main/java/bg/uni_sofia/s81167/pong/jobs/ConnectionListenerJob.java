package bg.uni_sofia.s81167.pong.jobs;

import static org.quartz.TriggerBuilder.newTrigger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bg.uni_sofia.s81167.dao.UserDAO;
import bg.uni_sofia.s81167.pong.game.GameContext;
import bg.uni_sofia.s81167.pong.model.GameConnection;

public class ConnectionListenerJob implements Job{

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionListenerJob.class);
	private static final int PORT = 10514;
	private final ServerSocket serverSocket;
	private boolean running = true;
	private ConcurrentHashMap<String, GameConnection> activeGames;
	private Set<String> activeUsers;
	private UserDAO userDAO;
	
	public ConnectionListenerJob() throws IOException{
		this.serverSocket = new ServerSocket(PORT);
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		setContext(context);
		while(running){
			try {
				Socket socket = serverSocket.accept();
				Scheduler scheduler = context.getScheduler();
				scheduleNewAuthenticationJob(scheduler, socket);
			} catch (IOException e) {
				LOGGER.error("Error while acceptiong new connection", e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void setContext(JobExecutionContext context) {
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		this.activeGames = (ConcurrentHashMap<String, GameConnection>) jobDataMap.get("activeGames");
		this.activeUsers = (Set<String>) jobDataMap.get("activeUsers");
		this.userDAO = (UserDAO) jobDataMap.get("userDAO");
	}

	private void scheduleNewAuthenticationJob(Scheduler scheduler, Socket socket) {
		JobKey jobKey= new JobKey(JobKey.createUniqueName("authenticator"));
		TriggerKey triggerKey = new TriggerKey(TriggerKey.createUniqueName("authenticator"));
		
		JobDetail authenticationJob =  buildJobDetail(jobKey, socket);
		Trigger authenticationTrigger = buildJobTrigger(triggerKey);
		
		attemptToScheduleJob(scheduler, socket, authenticationJob, authenticationTrigger);
	}

	private void attemptToScheduleJob(Scheduler scheduler, Socket socket, JobDetail authenticationJob,
			Trigger authenticationTrigger) {
		try {
			scheduler.scheduleJob(authenticationJob, authenticationTrigger);
		} catch (SchedulerException e) {
			LOGGER.error("Error while scheduling authenticator job", e);
			attemptToCloseSocket(socket);
		}
	}

	private Trigger buildJobTrigger(TriggerKey triggerKey) {
		return newTrigger()
				.withIdentity(triggerKey)
				.startNow()
				.build();
	}

	private JobDetail buildJobDetail(JobKey jobKey, Socket socket) {
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("activeGames", activeGames);
		jobDataMap.put("socket", socket);
		jobDataMap.put("userDAO", userDAO);
		jobDataMap.put("activeUsers", activeUsers);
		return JobBuilder.newJob(AuthenticationJob.class)
				.withIdentity(jobKey)
				.setJobData(jobDataMap)
				.build();
	}

	private void attemptToCloseSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException ie) {
			LOGGER.error("Error while trying to close socket.", ie);
		}
	}

}

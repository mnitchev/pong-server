package bg.uni_sofia.s81167.pong.jobs;

import static org.quartz.TriggerBuilder.newTrigger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
import bg.uni_sofia.s81167.model.User;
import bg.uni_sofia.s81167.pong.game.GameContext;

public class AuthenticationJob implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationJob.class);
	private static final String OK_RESPONSE = "OK";
	private static final String USER_NOT_FOUND = "NOT_FOUND";
	private ConcurrentHashMap<String, GameContext> activeGames;
	private ConcurrentHashMap<String, String> activeUsers;
	private boolean clientAuthenticated = false;
	private UserDAO userDAO;
	private Socket socket;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		setContext(context);
		try {
			PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while (!clientAuthenticated) {
				User user = readUserFromClient(socketReader);
				authenticateUser(context, socketWriter, user);
			}
		} catch (IOException e) {
			LOGGER.error("Error while reading from socket.", e);
			attemptToCloseSocket();
		} catch (SchedulerException e) {
			LOGGER.error("Error while trying to start connection job.", e);
			attemptToCloseSocket();
		}
	}

	private User readUserFromClient(BufferedReader socketReader) throws IOException {
		User user = new User();
		LOGGER.debug("Reading username");
		user.username = socketReader.readLine();
		LOGGER.debug("Username is : " + user.username + " Reading password");
		user.password = socketReader.readLine();
		LOGGER.debug("Password is : " + user.password);

		return user;
	}

	private void authenticateUser(JobExecutionContext context, PrintWriter socketWriter, User user)
			throws SchedulerException {
		LOGGER.debug("Authenticating user : " + user.username + " " + user.password);
		if (userExists(user)) {
			LOGGER.debug("User exists. Sending response.");
			clientAuthenticated = true;
			socketWriter.println(OK_RESPONSE);
			Scheduler scheduler = context.getScheduler();
			startClientConnectionJob(scheduler);
		} else {
			LOGGER.debug("User not found. Sending response.");
			socketWriter.println(USER_NOT_FOUND);
		}
		LOGGER.debug("Response sent.");
	}

	private boolean userExists(User user) {
		return true;
	}

	private void startClientConnectionJob(Scheduler scheduler) throws SchedulerException {
		LOGGER.debug("Starting connection job");
		JobDataMap dataMap = createDataMap();
		
		JobDetail connectionJob = JobBuilder.newJob(ClientConnectionJob.class)
				.withIdentity(JobKey.createUniqueName("connections")).setJobData(dataMap).build();

		Trigger connectionTrigger = newTrigger().withIdentity(TriggerKey.createUniqueName("connections")).startNow()
				.build();

		scheduler.scheduleJob(connectionJob, connectionTrigger);
		LOGGER.debug("Job started.");
	}

	private JobDataMap createDataMap() {
		JobDataMap dataMap = new JobDataMap();
		dataMap.put("socket", socket);
		dataMap.put("activeGames", activeGames);
		dataMap.put("activeUsers", activeUsers);
		return dataMap;
	}

	private void attemptToCloseSocket() throws JobExecutionException {
		try {
			this.clientAuthenticated = true;
			this.socket.close();
		} catch (IOException e) {
			LOGGER.error("Error while trying to close socket", e);
			throw new JobExecutionException();
		}
	}

	@SuppressWarnings("unchecked")
	private void setContext(JobExecutionContext context) {
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		this.socket = (Socket) jobDataMap.get("socket");
		this.activeGames = (ConcurrentHashMap<String, GameContext>) jobDataMap.get("activeGames");
		this.activeUsers = (ConcurrentHashMap<String, String>) jobDataMap.get("activeUsers");
	}

}

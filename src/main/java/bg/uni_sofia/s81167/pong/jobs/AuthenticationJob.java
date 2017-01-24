package bg.uni_sofia.s81167.pong.jobs;

import static org.quartz.TriggerBuilder.newTrigger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
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
import bg.uni_sofia.s81167.model.User;
import bg.uni_sofia.s81167.pong.model.GameConnection;

public class AuthenticationJob implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationJob.class);
	private static final String OK_RESPONSE = "OK";
	private static final String USER_NOT_FOUND = "NOT_FOUND";
	
	private boolean clientAuthenticated = false;
	private ConcurrentHashMap<String, GameConnection> activeGames;
	private Set<String> activeUsers;
	private UserDAO userDAO;
	private Socket socket;
	private User user;

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
		user.username = socketReader.readLine();
		user.password = socketReader.readLine();

		return user;
	}

	private void authenticateUser(JobExecutionContext context, PrintWriter socketWriter, User user)
			throws SchedulerException {
		if (userAuthenticated(user)) {
			activeUsers.add(user.username);
			this.user = user;
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

	private boolean userAuthenticated(User user) {
		try {
			if(activeUsers.contains(user.username)){
				return false;
			}
			if(!userDAO.userNameExists(user)){
				userDAO.addUser(user);
				return true;
			}else{
				return userDAO.userAuthenticated(user);
			}
		} catch ( SQLException e) {
			LOGGER.error("Database error. If problem persists restart server.", e);
			return false;
		}
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
		dataMap.put("username", user.username);
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
		this.activeGames = (ConcurrentHashMap<String, GameConnection>) jobDataMap.get("activeGames");
		this.activeUsers = (Set<String>) jobDataMap.get("activeUsers");
		this.userDAO = (UserDAO) jobDataMap.get("userDAO");
	}

}

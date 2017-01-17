package bg.uni_sofia.s81167.pong.jobs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
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
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bg.uni_sofia.s81167.pong.game.Command;
import bg.uni_sofia.s81167.pong.game.GameContext;
import bg.uni_sofia.s81167.pong.game.IGameUpdater;

public class ClientConnectionJob implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionJob.class);
	private static final String NEW_GAME_REQUEST = "NEW_GAME";
	private static final String ID_NOT_VALID = "NO";
	private static final String OK_RESPONSE = "OK";
	private IGameUpdater gameUpdater;
	private ConcurrentHashMap<String, GameContext> activeGames;
	private ConcurrentHashMap<String, String> activeUsers;
	private Socket socket;
	private boolean running = true;
	private String gameKey;
	private String userName;
	private BufferedReader socketReader;
	private PrintWriter socketWriter;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		setContext(context);
		try {
			socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			socketWriter = new PrintWriter(socket.getOutputStream(), true);
			startGameCoordinatorJob(context);
			play();
		} catch (IOException | SchedulerException e) {
			LOGGER.warn("User disconnected unexpectedly.", e);
			deactivateUser();
			attemtToCloseSocket();
			throw new JobExecutionException();
		}
	}

	private void deactivateUser() {
		activeUsers.remove(userName);
	}

	private void startGameCoordinatorJob(JobExecutionContext context) throws SchedulerException {
		Scheduler scheduler = context.getScheduler();
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("gameKey", gameKey);
		jobDataMap.put("activeGames", activeGames);
		JobDetail gameCoordinatorJob = JobBuilder.newJob(GameCoordinatorJob.class)
				.withIdentity(JobKey.createUniqueName("gameCoordinators")).setJobData(jobDataMap).build();
		Trigger gameCoordinatorTrigger = TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.createUniqueName("gameCoordinators")).build();

		scheduler.scheduleJob(gameCoordinatorJob, gameCoordinatorTrigger);

	}

	private void connectToUser() throws IOException {
		waitForValidKey();
		waitForValidGame();
	}

	private void waitForValidGame() {
		while (!isGameValid()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			LOGGER.debug("Game is not valid");
		}
		LOGGER.debug("Game is valid");
		socketWriter.println(OK_RESPONSE);
	}

	private void waitForValidKey() throws IOException {
		while (!isGameKeyValid()) {
			String gameId = socketReader.readLine();
			String response = processGameRequest(gameId);
			socketWriter.println(OK_RESPONSE);
			socketWriter.println(response);
		}
	}

	private boolean isGameValid() {
		GameContext context = activeGames.get(gameKey);
		return context.isGameValid();
	}

	private String processGameRequest(String gameId) {
		if (NEW_GAME_REQUEST.equals(gameId)) {
			return createNewGame();
		} else {
			GameContext context = activeGames.get(gameId);
			if (context != null) {
				if (context.setNextPlayerSocket(socket)) {
					return gameId;
				}
			}
		}
		return ID_NOT_VALID;
	}

	private String createNewGame() {
		String gameId = UUID.randomUUID().toString();
		activeGames.put(gameId, new GameContext());
		return gameId;
	}

	private boolean isGameKeyValid() {
		return gameKey != null;
	}

	private void play() throws IOException, JobExecutionException {
		while (running) {
			String command = getUserInput();
			reinstanceIfUserDisconnected(command);
			interpretCommand(command);
		}
	}

	private void reinstanceIfUserDisconnected(String command) throws IOException {
		if (command == null) {
			sendReconnectResponse();
			connectToUser();
		}
	}

	private void sendReconnectResponse() throws IOException {
		PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
		socketWriter.println(Command.RECONNECT.toStringRepresentation());
	}

	private void attemtToCloseSocket() {
		try {
			socket.close();
		} catch (IOException e) {
			LOGGER.error("Error while trying to close socket.", e);
		}
	}

	private void interpretCommand(String commandAsString) throws JobExecutionException {
		Command command = Command.toCommand(commandAsString);
		if (command == null) {
			attemtToCloseSocket();
			throw new JobExecutionException();
		}
		if (command == Command.DISCONNECT) {
			gameUpdater.updateGameContext(gameKey, userName, command);
			deactivateUser();
			running = false;
			attemtToCloseSocket();
		} else {
			gameUpdater.updateGameContext(gameKey, userName, command);
		}
	}

	@SuppressWarnings("unchecked")
	private void setContext(JobExecutionContext context) {
		JobDataMap jobDetailMap = context.getJobDetail().getJobDataMap();
		this.socket = (Socket) jobDetailMap.get("socket");
		this.activeGames = (ConcurrentHashMap<String, GameContext>) jobDetailMap.get("activeGames");
		this.activeUsers = (ConcurrentHashMap<String, String>) jobDetailMap.get("activeUsers");
		this.userName = (String) jobDetailMap.get("username");
	}

	private String getUserInput() throws IOException {
		return socketReader.readLine();
	}

}

package bg.uni_sofia.s81167.pong.jobs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
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
import bg.uni_sofia.s81167.pong.model.GameConnection;

public class ClientConnectionJob implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionJob.class);
	private static final String NEW_GAME_REQUEST = "NEW_GAME";
	private static final String ID_NOT_VALID = "NO";
	private static final String OK_RESPONSE = "OK";
	private ConcurrentHashMap<String, GameConnection> activeGames;
	private Queue<Command> commandQueue;
	private Set<String> activeUsers;
	private Socket socket;
	private boolean running = true;
	private String gameId;
	private String username;
	private BufferedReader socketReader;
	private PrintWriter socketWriter;
	private boolean gameInitialized = false;
	private JobExecutionContext context;
	private boolean connectionHealthy = true;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		setContext(context);
		setConnection();
		while (connectionHealthy) {
			connectToGame();
		}
	}

	private void connectToGame() throws JobExecutionException {
		try {
			initializeGame();
			play();
		} catch (IOException | SchedulerException e) {
			LOGGER.warn("User disconnected unexpectedly.", e);
			attemtToCloseSocket();
			connectionHealthy = false;
			throw new JobExecutionException(e);
		}
	}

	private void setConnection() throws JobExecutionException {
		try {
			socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			socketWriter = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			LOGGER.info("User disconnected");
			connectionHealthy = false;
			running = false;
			gameInitialized = true;
			throw new JobExecutionException(e);
		}
	}

	private void initializeGame() throws IOException, SchedulerException {
		while (!gameInitialized) {
			LOGGER.debug("Waiting for request");
			String request = socketReader.readLine();
			LOGGER.debug("Received request : {}", request);
			if (NEW_GAME_REQUEST.equals(request)) {
				createNewGame();
				gameInitialized = true;
				socketWriter.println(OK_RESPONSE);
				socketWriter.println(gameId);
				startGameCoordinatorJob(context);
			} else {
				connectToExistingGame(request);
			}
		}
	}

	private void connectToExistingGame(String request) {
		if (activeGames.containsKey(request)) {
			LOGGER.debug("Game id found!");
			this.gameId = request;
			GameConnection connection = activeGames.get(request);
			if (!connection.isActive()) {
				gameInitialized = true;
				LOGGER.debug("Connecting to game");
				connection.activate(socket);
				activeGames.replace(gameId, connection);
				commandQueue = connection.getCommandQueue();
				socketWriter.println(OK_RESPONSE);
				socketWriter.println(gameId);
				return;
			}
		}
		LOGGER.debug("Game ID not found or already in connected.");
		socketWriter.println(ID_NOT_VALID);
	}

	private void createNewGame() {
		this.gameId = UUID.randomUUID().toString();
		activeGames.put(gameId, new GameConnection());
		LOGGER.debug("Adding game with id = {} to list of games", gameId);
		this.commandQueue = new LinkedList<>();
	}

	private void startGameCoordinatorJob(JobExecutionContext context) throws SchedulerException {
		Scheduler scheduler = context.getScheduler();
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("gameId", gameId);
		jobDataMap.put("activeGames", activeGames);
		jobDataMap.put("hostSocket", socket);
		jobDataMap.put("hostPlayerQueue", commandQueue);
		JobDetail gameCoordinatorJob = JobBuilder.newJob(GameCoordinatorJob.class)
				.withIdentity(JobKey.createUniqueName("gameCoordinators")).setJobData(jobDataMap).build();
		Trigger gameCoordinatorTrigger = TriggerBuilder.newTrigger()
				.withIdentity(TriggerKey.createUniqueName("gameCoordinators")).build();

		scheduler.scheduleJob(gameCoordinatorJob, gameCoordinatorTrigger);
		LOGGER.debug("Coordinator job started!");
	}

	private void play() throws IOException, JobExecutionException {
		LOGGER.debug("Reading user input");
		while (running) {
			String commandString = socketReader.readLine();
			LOGGER.debug("Received command = {}", commandString);
			int commandInt = Integer.parseInt(commandString);
			Command command = Command.toCommand(commandInt);
			synchronized (commandQueue) {
				commandQueue.offer(command);
			}
			if (command.toInt() == Command.DISCONNECT.toInt()) {
				running = false;
				gameInitialized = false;
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void setContext(JobExecutionContext context) {
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		this.socket = (Socket) jobDataMap.get("socket");
		this.activeGames = (ConcurrentHashMap<String, GameConnection>) jobDataMap.get("activeGames");
		this.context = context;
		this.activeUsers = (Set<String>) jobDataMap.get("activeUsers");
		this.username = (String) jobDataMap.get("username");
	}

	private void attemtToCloseSocket() {
		try {
			activeUsers.remove(username);
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			LOGGER.error("Error while trying to close socket.", e);
		}
	}

}

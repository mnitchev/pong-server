package bg.uni_sofia.s81167.pong.jobs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bg.uni_sofia.s81167.pong.game.Command;
import bg.uni_sofia.s81167.pong.game.GameContext;
import bg.uni_sofia.s81167.pong.model.Ball;
import bg.uni_sofia.s81167.pong.model.GameConnection;
import bg.uni_sofia.s81167.pong.model.Player;

public class GameCoordinatorJob implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameCoordinatorJob.class);

	private static final String OK_STATUS = "OK";
	private static final String DISCONNECT_STATUS = "DISCONNECT";
	private static final String PAUSE_STATUS = "PAUSED";
	private String status = PAUSE_STATUS;

	private Socket hostSocket;
	private Queue<Command> hostPlayerQueue;

	private Socket playerSocket;
	private Queue<Command> playerQueue;

	private String gameId;
	private ConcurrentHashMap<String, GameConnection> activeGames;
	private GameContext gameContext;

	private PrintWriter hostWriter;
	private PrintWriter playerWriter;

	private boolean running = true;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		setContext(context.getJobDetail().getJobDataMap());
		createHostWriter();
		createGameContext();
		while (running) {
			waitForNextBroadcast();
			checkIfUsersDisconnected();
			processInputFromPlayers();
			updateBall();
			sendUpdateToPlayers();
		}
		LOGGER.debug("Stopping game coordinator.");
		activeGames.remove(gameId);
	}

	private void checkIfUsersDisconnected() throws JobExecutionException {
		if (hostSocket != null && hostSocket.isClosed()) {
			LOGGER.info("Host user disconnected");
			disconnectHost();
		}
		if (playerSocket != null && playerSocket.isClosed()) {
			removePlayerResources();
		}
	}

	private void waitForNextBroadcast() {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			LOGGER.error("Interrupted exception. This should never happen, but shouldn't be fatal if only once.", e);
		}
	}

	private void sendUpdateToPlayers() throws JobExecutionException {
		if (hostSocket != null) {
			sendGameInformationToHost();
		}
		if (playerSocket == null) {
			this.status = PAUSE_STATUS;
			checkIfPlayerConnected();
		} else {
			if (playerWriter == null) {
				createPlayerWriter();
			}
			sendGameInformationToPlayer();
		}
	}

	private void sendGameInformationToHost() throws JobExecutionException {
		hostWriter.println(status);
		Player left = gameContext.getLeftPlayer();
		sendPlayerPosition(left, hostWriter);
		Player right = gameContext.getRightPlayer();
		sendPlayerPosition(right, hostWriter);
		sendBall(hostWriter);
	}

	private void sendGameInformationToPlayer() throws JobExecutionException {
		playerWriter.println(status);
		Player left = gameContext.getLeftPlayer();
		sendPlayerPosition(left, playerWriter);
		Player right = gameContext.getRightPlayer();
		sendPlayerPosition(right, playerWriter);
		sendBall(playerWriter);
	}

	private void updateBall() {
		if (!PAUSE_STATUS.equals(status)) {
			gameContext.updateBall();
		}
	}

	private void processInputFromPlayers() {
		processHostInput();
		processRightPlayerInpit();
	}

	private void checkIfPlayerConnected() {
		GameConnection connection = activeGames.get(gameId);
		playerQueue = connection.getCommandQueue();
		playerSocket = connection.getSocket();
	}

	private void processHostInput() {
		Command command;
		synchronized (hostPlayerQueue) {
			command = hostPlayerQueue.poll();
		}
		if (command != null) {
			checkIfHostWantsToDisconenct(command);
			gameContext.updateLeftPlayer(command);
		}
	}

	private void processRightPlayerInpit() {
		if (playerQueue == null) {
			checkIfPlayerConnected();
		} else {
			if (!playerQueue.isEmpty()) {
				Command command;
				synchronized (playerQueue) {
					command = playerQueue.poll();
				}
				checkIfPlayerWantsToDisconnect(command);
				gameContext.updateRightPlayer(command);
			}
		}
	}

	private void checkIfPlayerWantsToDisconnect(Command command) {
		if (command == Command.DISCONNECT) {
			removePlayerResources();
		}
	}

	private void checkIfHostWantsToDisconenct(Command command) {
		if (command.toInt() == Command.DISCONNECT.toInt()) {
			disconnectHost();
		}
	}

	private void disconnectHost() {
		status = DISCONNECT_STATUS;
		running = false;
		hostSocket = null;
		hostPlayerQueue = null;
	}

	private void createGameContext() {
		this.gameContext = new GameContext();
	}

	private void removePlayerResources() {
		LOGGER.debug("Removing player from game");
		this.playerSocket = null;
		this.playerQueue = null;
		this.playerWriter = null;
		GameConnection connection = activeGames.get(gameId);
		connection.deactivate();
		activeGames.put(gameId, connection);
	}

	private void sendBall(PrintWriter writer) {
		Ball ball = gameContext.getBall();
		writer.println(ball.getX());
		writer.println(ball.getY());
	}

	private void sendPlayerPosition(Player player, PrintWriter writer) {
		writer.println(player.positionX);
		writer.println(player.positionY);
		writer.println(player.score);
	}

	private void setContext(JobDataMap jobDataMap) throws JobExecutionException {
		setGameId(jobDataMap);
		setSocket(jobDataMap);
		setLeftPlayerQueue(jobDataMap);
		setActiveGames(jobDataMap);
	}

	@SuppressWarnings("unchecked")
	private void setActiveGames(JobDataMap jobDataMap) throws JobExecutionException {
		this.activeGames = (ConcurrentHashMap<String, GameConnection>) jobDataMap.get("activeGames");
		if (activeGames == null) {
			LOGGER.error("Context initialization error. Active games not set.");
			throw new JobExecutionException();
		}
	}

	@SuppressWarnings("unchecked")
	private void setLeftPlayerQueue(JobDataMap jobDataMap) throws JobExecutionException {
		this.hostPlayerQueue = (Queue<Command>) jobDataMap.get("hostPlayerQueue");
		if (hostPlayerQueue == null) {
			LOGGER.error("Context initialization error. Left player queue not set");
			throw new JobExecutionException();
		}
	}

	private void setSocket(JobDataMap jobDataMap) throws JobExecutionException {
		this.hostSocket = (Socket) jobDataMap.get("hostSocket");
		if (hostSocket == null) {
			LOGGER.error("Context initialization error. Socket not set");
			throw new JobExecutionException();
		}
	}

	private void setGameId(JobDataMap jobDataMap) throws JobExecutionException {
		this.gameId = (String) jobDataMap.get("gameId");
		if (gameId == null) {
			LOGGER.error("Context initialization error. Game id not set.");
			throw new JobExecutionException();
		}
	}

	private void createHostWriter() throws JobExecutionException {
		try {
			this.hostWriter = new PrintWriter(hostSocket.getOutputStream(), true);
		} catch (IOException e) {
			LOGGER.error("Client disconencted!", e);
			throw new JobExecutionException(e);
		}
	}

	private void createPlayerWriter() {
		try {
			playerWriter = new PrintWriter(playerSocket.getOutputStream(), true);
			this.status = OK_STATUS;
		} catch (IOException e) {
			LOGGER.error("Player disconnected. Reseting game", e);
			removePlayerResources();
		}
	}

}
package bg.uni_sofia.s81167.pong.jobs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.experimental.theories.Theories;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bg.uni_sofia.s81167.pong.game.Command;
import bg.uni_sofia.s81167.pong.game.GameContext;
import bg.uni_sofia.s81167.pong.model.Ball;
import bg.uni_sofia.s81167.pong.model.Player;

public class GameCoordinatorJob implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameCoordinatorJob.class);

	private Socket hostSocket;
	private ConcurrentLinkedQueue<Command> hostPlayerQueue;

	private Socket playerSocket;
	private ConcurrentLinkedQueue<Command> playerQueue;

	private ConcurrentHashMap<String, Socket> playerSocketConnections;
	private ConcurrentHashMap<String, ConcurrentLinkedQueue<Command>> playerConnectionQueues;
	private GameContext gameContext;
	private String gameId;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		setContext(context.getJobDetail().getJobDataMap());
		createGameContext();
		while (true) {
			processInputFromPlayers();
			updateBall();
			sendUpdateToPlayers();
		}
	}

	private void sendUpdateToPlayers() throws JobExecutionException {
		sendGameInformationToHost();
		if (playerSocket == null) {
			checkIfPlayerConnected();
		} else {
			sendGameInformationToPlayer();
		}
	}

	private void sendGameInformationToHost() {
		try {
			PrintWriter writer = new PrintWriter(hostSocket.getOutputStream());
			Player left = gameContext.getLeftPlayer();
			sendPlayerPosition(left, writer);
			Player right = gameContext.getRightPlayer();
			sendPlayerPosition(right, writer);
			sendBall(writer);
		} catch (IOException e) {
			LOGGER.error("Player disconnected. Reseting game", e);
			throw new JobExecutionException();
		}	
	}

	private void sendGameInformationToPlayer() throws JobExecutionException {
		try {
			PrintWriter writer = new PrintWriter(playerSocket.getOutputStream());
			Player left = gameContext.getLeftPlayer();
			sendPlayerPosition(left, writer);
			Player right = gameContext.getRightPlayer();
			sendPlayerPosition(right, writer);
			sendBall(writer);
		} catch (IOException e) {
			LOGGER.error("Player disconnected. Reseting game", e);
			throw new JobExecutionException();
		}
	}

	private void sendBall(PrintWriter writer) {
		Ball ball = gameContext.getBall();
		writer.println(ball.getX());
		writer.println(ball.getY());
	}

	private void sendPlayerPosition(Player left, PrintWriter writer) {
		writer.println(left.positionX);
		writer.println(left.positionY);
	}

	private void updateBall() {
		gameContext.updateBall();
	}

	private void processInputFromPlayers() {
		processLeftPlayerInput();
		processRightPlayerInpit();
	}

	private void processRightPlayerInpit() {
		if (hostPlayerQueue == null) {
			checkIfPlayerConnected();
		} else {
			if (!playerQueue.isEmpty()) {
				Command command = playerQueue.remove();
				gameContext.updateRightPlayer(command);
			}
		}
	}

	private void checkIfPlayerConnected() {
		playerQueue = playerConnectionQueues.get(gameId);
		playerSocket = playerSocketConnections.get(gameId);
	}

	private void processLeftPlayerInput() {
		if (!hostPlayerQueue.isEmpty()) {
			Command command = hostPlayerQueue.remove();
			gameContext.updateLeftPlayer(command);
		}
	}

	private void createGameContext() {
		this.gameContext = new GameContext();
	}

	@SuppressWarnings("unchecked")
	private void setContext(JobDataMap jobDataMap) throws JobExecutionException {
		setSocket(jobDataMap);
		setLeftPlayerQueue(jobDataMap);
		setRightPlayerSockets(jobDataMap);
		setConnectionQueues(jobDataMap);
		setGameId(jobDataMap);
	}

	@SuppressWarnings("unchecked")
	private void setRightPlayerSockets(JobDataMap jobDataMap) throws JobExecutionException {
		this.playerSocketConnections = (ConcurrentHashMap<String, Socket>) jobDataMap.get("rightSocketConnections");
		if (playerSocketConnections == null) {
			LOGGER.error("Context initialization error. Right socket connections not set.");
			throw new JobExecutionException();
		}
	}

	@SuppressWarnings("unchecked")
	private void setConnectionQueues(JobDataMap jobDataMap) throws JobExecutionException {
		this.playerConnectionQueues = (ConcurrentHashMap<String, ConcurrentLinkedQueue<Command>>) jobDataMap
				.get("playerConnectionQueues");
		if (playerConnectionQueues == null) {
			LOGGER.error("Context initialization error. Game connection queues not set");
			throw new JobExecutionException();
		}
	}

	@SuppressWarnings("unchecked")
	private void setLeftPlayerQueue(JobDataMap jobDataMap) throws JobExecutionException {
		this.hostPlayerQueue = (ConcurrentLinkedQueue<Command>) jobDataMap.get("hostPlayerQueue");
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

	/*
	 * private static final Logger LOGGER =
	 * LoggerFactory.getLogger(GameCoordinatorJob.class); private String gameId;
	 * private ConcurrentHashMap<String, ConcurrentLinkedQueue<Command>>
	 * activeGames; public Socket left; public Socket right;
	 * 
	 * @Override public void execute(JobExecutionContext context) throws
	 * JobExecutionException { setContext(context); sendGameInformation(); }
	 * 
	 * private void sendGameInformation() throws JobExecutionException {
	 * GameContext gameContext = activeGames.get(gameId); Socket
	 * leftPlayerSocket = gameContext.getLeftPlayerSocket(); Socket
	 * rightPlayerSocket = gameContext.getRightPlayerSocket(); try {
	 * sendToPlayer(leftPlayerSocket, gameContext);
	 * sendToPlayer(rightPlayerSocket, gameContext); } catch (IOException e) {
	 * LOGGER.warn("Client disconnected!", e); throw new
	 * JobExecutionException(e); } }
	 * 
	 * private void sendToPlayer(Socket rightPlayerSocket, GameContext
	 * gameContext) throws IOException { PrintWriter socketWriter = new
	 * PrintWriter(rightPlayerSocket.getOutputStream(), true); Player left =
	 * gameContext.getLeftPlayer(); Player right = gameContext.getRightPlayer();
	 * sendPlayerPosition(socketWriter, left); sendPlayerPosition(socketWriter,
	 * right); sendBallPosition(socketWriter, gameContext);
	 * sendScore(socketWriter, left, right); }
	 * 
	 * private void sendScore(PrintWriter socketWriter, Player left, Player
	 * right) { socketWriter.println(left.score);
	 * socketWriter.println(right.score); }
	 * 
	 * private void sendBallPosition(PrintWriter socketWriter, GameContext
	 * gameContext) { Ball ball = gameContext.getBall();
	 * socketWriter.println(ball.getX()); socketWriter.println(ball.getY()); }
	 * 
	 * private void sendPlayerPosition(PrintWriter socketWriter, Player player)
	 * { socketWriter.println(player.positionX);
	 * socketWriter.println(player.positionY); }
	 * 
	 * @SuppressWarnings("unchecked") private void
	 * setContext(JobExecutionContext context) { JobDataMap jobDataMap =
	 * context.getJobDetail().getJobDataMap(); this.activeGames =
	 * (ConcurrentHashMap<String, GameContext>) jobDataMap.get("gameContext");
	 * this.gameId = (String) jobDataMap.get("gameId"); }
	 */
}

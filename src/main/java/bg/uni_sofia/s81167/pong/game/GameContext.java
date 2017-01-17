package bg.uni_sofia.s81167.pong.game;

import java.net.Socket;

import bg.uni_sofia.s81167.pong.model.Ball;
import bg.uni_sofia.s81167.pong.model.Player;

public class GameContext {
	
	public static final int LEFT_PLAYER_X = 20;
	public static final int RIGHT_PLAYER_X = 1004;
	public static final int CENTER_Y = 384;
	public static final int STEP = 5;
	public static final int RECTANGLE_X = 10;
	public static final int RECTANGLE_Y = 50;
	public static final int WINDOW_WIDTH = 1024;
	public static final int WINDOW_HEIGHT = 768;
	private Player left;
	private Player right;
	private Ball ball;
	private Socket leftPlayerSocket;
	private Socket rightPlayerSocket;
	
	public GameContext(){
		this.left = new Player();
		left.positionX = LEFT_PLAYER_X;
		left.positionY = CENTER_Y;
		this.right = new Player();
		right.positionX = RIGHT_PLAYER_X;
		right.positionY = CENTER_Y;
		this.ball = new Ball();
	}
	
	public void updatePlayer(String username, Command command) {
		if(left.username.equals(username)){
			updatePosition(left, command);
		}else if(right.username.equals(username)){
			updatePosition(right, command);
		}
	}
	
	private void updatePosition(Player player, Command command) {
		if(command == Command.UP){
			if(player.positionY - STEP >= 0){
				player.positionY -= STEP;
			}else{
				player.positionY = 0;
			}
		}else if(command == Command.DOWN){
			if(player.positionY - STEP <= WINDOW_HEIGHT){
				player.positionY += STEP;
			}else{
				player.positionY = WINDOW_HEIGHT;
			}
		}
	
	}
	
	public boolean isGameValid(){
		return leftPlayerSocket != null && rightPlayerSocket != null;
	}
	
	public void updateBall(){
		ball.moveBall(left, right);
	}

	public Player getLeftPlayer() {
		return left;
	}
		
	public Player getRightPlayer() {
		return right;
	}

	public Ball getBall() {
		return ball;
	}

	public Socket getLeftPlayerSocket() {
		return leftPlayerSocket;
	}
	
	public Socket getRightPlayerSocket() {
		return rightPlayerSocket;
	}
	
	private void setLeftPlayerSocket(Socket leftPlayer) {
		this.leftPlayerSocket = leftPlayer;
	}
	
	private void setRightPlayerSocket(Socket rightPlayer) {
		this.rightPlayerSocket = rightPlayer;
	}

	public boolean setNextPlayerSocket(Socket socket) {
		if(leftPlayerSocket == null){
			setLeftPlayerSocket(socket);
			return true;
		}
		if(rightPlayerSocket == null){
			setRightPlayerSocket(socket);
			return true;
		}
		return false;
	}

	public void updateLeftPlayer(Command command) {
		// TODO Auto-generated method stub
		
	}

	public void updateRightPlayer(Command command) {
		// TODO Auto-generated method stub
		
	}
}

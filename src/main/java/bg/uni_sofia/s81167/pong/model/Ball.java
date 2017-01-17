package bg.uni_sofia.s81167.pong.model;

import bg.uni_sofia.s81167.pong.game.GameContext;

public class Ball {
	private int positionX;
	private int positionY;
	private int direction;
	private int radius;
	
	public Ball(){
		this.positionX = GameContext.CENTER_Y;
		this.positionX = GameContext.LEFT_PLAYER_X + 10;
	}

	public void moveBall(Player left, Player right) {
		
	}

	public int getX() {
		return positionX;
	}
	
	public int getY(){
		return positionY;
	}
}

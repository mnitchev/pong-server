package bg.uni_sofia.s81167.pong.model;

import java.awt.Rectangle;

import bg.uni_sofia.s81167.pong.game.GameContext;

public class Ball {
	private int positionX;
	private int positionY;
	private int xa = 3;
	private int ya = 3;
	private int RADIUS = 20;

	public Ball() {
		this.positionX = GameContext.CENTER_X;
		this.positionY = GameContext.CENTER_Y;
	}

	public void moveBall(Player left, Player right) {
		positionX += xa;
		positionY += ya;
		if (positionX < 0) {
			right.score++;
			positionX = GameContext.WINDOW_WIDTH/ 2;
			xa = -xa;
		} else if (positionX > GameContext.WINDOW_WIDTH - RADIUS - 7) {
			left.score++;
			positionX = GameContext.WINDOW_WIDTH / 2;
			xa = -xa;
		} else if (positionY < 0 || positionY > GameContext.WINDOW_HEIGHT - RADIUS - 29){
			ya = -ya;
		}
		Rectangle leftPlayer = new Rectangle(left.positionX, left.positionY, 20, 100);
		Rectangle rightPlayer = new Rectangle(right.positionX, right.positionY, 20, 100);
		checkCollision(leftPlayer, rightPlayer);
	}

	public void checkCollision(Rectangle leftPlayer, Rectangle rightPlayer) {
		if (leftPlayer.getBounds().intersects(getBounds())
				|| rightPlayer.getBounds().intersects(getBounds()))
			xa = -xa;
	}

	public Rectangle getBounds() {
		return new Rectangle(positionX, positionY, RADIUS, RADIUS);
	}

	public int getX() {
		return positionX;
	}

	public int getY() {
		return positionY;
	}
}

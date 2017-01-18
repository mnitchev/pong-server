package bg.uni_sofia.s81167.pong.game;

public enum Command {

	DISCONNECT(1), UP(200), DOWN(208);

	private final int id;

	private Command(int id) {
		this.id = id;
	}

	public int toInt() {
		return id;
	}

	public static Command toCommand(int command) {
		switch (command) {
		case 1:
			return Command.DISCONNECT;
		case 200:
			return Command.UP;
		case 208:
			return Command.DOWN;
		default:
			return null;
		}
	}
}

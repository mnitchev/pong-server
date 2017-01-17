package bg.uni_sofia.s81167.pong.game;

public enum Command {

	DISCONNECT("disconnect"), UP("up"), DOWN("down"), SERVE("serve"), RECONNECT("reconnect");

	private final String id;

	private Command(String id) {
		this.id = id;
	}

	public String toStringRepresentation() {
		return id;
	}

	public static Command toCommand(String command) {
		switch (command) {
		case "disconnect":
			return Command.DISCONNECT;
		case "up":
			return Command.UP;
		case "down":
			return Command.DOWN;
		case "serve":
			return Command.SERVE;
		default:
			return null;
		}
	}
}

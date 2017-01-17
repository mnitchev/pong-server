package bg.uni_sofia.s81167.pong.game;

public interface IGameUpdater {
	void updateGameContext(String key, String username, Command command);

	String createJobContext(String sourceUsername, String targetUsername);
}

package bg.uni_sofia.s81167.dao;

import java.io.IOException;

import bg.uni_sofia.s81167.model.User;

public interface UserDAO {
	void addUser(User user) throws IOException;
	boolean userExists(User user);
}

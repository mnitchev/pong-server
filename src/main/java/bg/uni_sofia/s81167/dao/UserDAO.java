package bg.uni_sofia.s81167.dao;

import java.io.IOException;
import java.sql.SQLException;

import bg.uni_sofia.s81167.model.User;

public interface UserDAO {
	void addUser(User user) throws SQLException;
	boolean userNameExists(User user) throws SQLException;
	boolean userAuthenticated(User user) throws SQLException;
}

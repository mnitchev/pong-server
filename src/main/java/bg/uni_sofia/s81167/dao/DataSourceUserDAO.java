package bg.uni_sofia.s81167.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.NamingException;

import org.apache.commons.dbcp.BasicDataSource;

import bg.uni_sofia.s81167.model.User;

public class DataSourceUserDAO implements UserDAO {
	private BasicDataSource dataSource;
	private String tableName = "PONGUSERS";
	private Encryptor encryptor;

	public DataSourceUserDAO() throws NamingException, SQLException {
		this.encryptor = new Encryptor();
		dataSource = new BasicDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUsername("root");
		dataSource.setPassword("Password123");
		dataSource.setUrl("jdbc:mysql://localhost:3306/Pong?useSSL=false");
		dataSource.setMaxActive(10);
		dataSource.setMaxIdle(5);
		dataSource.setInitialSize(5);
		dataSource.setValidationQuery("SELECT 1");

		initializeDatabase();
	}

	private void initializeDatabase() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			if (tableExists(connection)) {
				return;
			}
			PreparedStatement statement = connection.prepareStatement(
					"CREATE TABLE " + tableName + " (" + "username VARCHAR(25) , password VARCHAR(100) )");
			statement.executeUpdate();
		}
	}

	private boolean tableExists(Connection connection) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		try (ResultSet tables = metadata.getTables(null, null, tableName, null)) {
			if (tables.next()) {
				return true;
			}
			return false;
		}
	}

	@Override
	public void addUser(User user) throws SQLException {
		encryptUserPassword(user);
		addUserToDatabase(user);
	}

	private void addUserToDatabase(User user) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection
					.prepareStatement("INSERT INTO " + tableName + " VALUES ( ? , ? ) ;");
			statement.setString(1, user.username);
			statement.setString(2, user.password);

			statement.executeUpdate();
		}
	}

	private void encryptUserPassword(User user) {
		user.password = encryptor.encrypt(user.password);
	}

	@Override
	public boolean userNameExists(User user) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection
					.prepareStatement("SELECT * FROM " + tableName + " WHERE username = ? ;");
			statement.setString(1, user.username);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean userAuthenticated(User user) throws SQLException {
		encryptUserPassword(user);
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection
					.prepareStatement("SELECT * FROM " + tableName + " WHERE username = ? " + "AND password = ? ;");
			statement.setString(1, user.username);
			statement.setString(2, user.password);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return true;
			}
		}
		return false;
	}
}

package bg.uni_sofia.s81167.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import bg.uni_sofia.s81167.model.User;

public class DataSourceUserDAO implements UserDAO{
	private DataSource dataSource;
	
	public DataSourceUserDAO(String resourceName) throws NamingException{
		Context context =  new InitialContext(); 
		dataSource = (DataSource) context.lookup(resourceName);
	}

	@Override
	public void addUser(User user) {
		try(Connection connection = dataSource.getConnection()){
			PreparedStatement statement = connection.prepareStatement("INSERT INTO USERS VALUES ( ? , ? ) ;");
			statement.setString(1, user.username);
			statement.setString(2, user.password);
			
			statement.executeQuery();
		}catch(SQLException e){
			
		}
	}

	@Override
	public boolean userExists(User user){
		try(Connection connection = dataSource.getConnection()){
			PreparedStatement statement = connection.prepareStatement("SELECT * WHERE userName = ? "
					+ "AND password = ? ;");
			statement.setString(1, user.username);
			statement.setString(2, user.password);
			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next()){
				return true;
			}
		}catch(SQLException e){
			
		}
		return false;
	}
}

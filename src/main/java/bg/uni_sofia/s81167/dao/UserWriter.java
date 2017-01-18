package bg.uni_sofia.s81167.dao;

import static java.nio.file.Files.exists;
import static java.nio.file.Paths.get;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bg.uni_sofia.s81167.model.User;

public class UserWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserWriter.class);
	private final File file;
	private final Encryptor encryptor;
	
	public UserWriter(String filePath, Encryptor encryptor) throws IOException {
		this.file = createFileIfNeeded(filePath);
		this.encryptor = encryptor;
	}
	private File createFileIfNeeded(String filePath) throws IOException {
		Path path = get(filePath);
		if(!exists(path)){
			return path.toFile();
		}
		return new File(filePath);
	}

	public synchronized void addUser(User user) {
		try (PrintWriter writer = new PrintWriter(file)) {
			String userAsString = convertUserToString(user);
			writer.println(userAsString);
		} catch (FileNotFoundException e) {
			LOGGER.error("Could not find specified file while writing.");
			throw new RuntimeException(e);
		}
	}
	
	public synchronized boolean userExists(User user){
		
		return false;
	}

	private String convertUserToString(User user) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(user.username);
		stringBuilder.append(" ");
		String encryptedPassword = encryptor.encrypt(user.password);
		stringBuilder.append(encryptedPassword);
		String userAsString = stringBuilder.toString();
		return userAsString;
	}

}

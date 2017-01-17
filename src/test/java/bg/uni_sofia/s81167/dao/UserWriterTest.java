package bg.uni_sofia.s81167.dao;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import bg.uni_sofia.s81167.model.User;

@RunWith(MockitoJUnitRunner.class)
public class UserWriterTest {

	private static final String USERNAME = "OptimusPrime";
	private static final String PASSWORD_PLAIN = "password123";
	private static final String PASSWORD_ENCRYPTED = "asdasdasdasdasdasdasdasdasdasd";
	private static final String FILE_PATH = "C:\\workspace\\chat\\users.txt";
	private User user = new User();
	private UserWriter userWriter;
	
	@Mock
	private Encryptor encryptor;
	
	@Before
	public void setUp() throws IOException{
		user.username = USERNAME;
		user.password = PASSWORD_PLAIN;
		userWriter = new UserWriter(FILE_PATH, encryptor);
	}
	
	@Test
	public void testAddNewUser() throws FileNotFoundException, IOException {
		when(encryptor.encrypt(PASSWORD_PLAIN)).thenReturn(PASSWORD_ENCRYPTED);
		userWriter.addUser(user);
		validateFileContents();
	}

	private void validateFileContents() throws FileNotFoundException, IOException {
		String line;
		line = readFile();
		assertThat(line, is(USERNAME + " " + PASSWORD_ENCRYPTED));
	}

	private String readFile() throws IOException, FileNotFoundException {
		String line;
		File file = new File(FILE_PATH);
		try(BufferedReader reader = new BufferedReader(new FileReader(file))){
			line = reader.readLine();
		}
		return line;
	}

}

package bg.uni_sofia.s81167.dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encryptor {
	public String encrypt(String password) {
		String generatedPassword;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] bytes = md.digest(password.getBytes());
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			generatedPassword = sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		return generatedPassword;
	}
}

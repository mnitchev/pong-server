package bg.uni_sofia.s81167.pong.exceptions;

public class PasswordIncorrectException extends Exception {
	private static final long serialVersionUID = 773472672745739872L;

	public PasswordIncorrectException(){
	}
	
	public PasswordIncorrectException(Throwable e){
		super(e);
	}
}

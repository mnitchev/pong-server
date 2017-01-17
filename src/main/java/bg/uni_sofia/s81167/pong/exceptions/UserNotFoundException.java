package bg.uni_sofia.s81167.pong.exceptions;

public class UserNotFoundException extends Exception {
	private static final long serialVersionUID = 773472672745739872L;

	public UserNotFoundException(){
	}
	
	public UserNotFoundException(Throwable e){
		super(e);
	}
}

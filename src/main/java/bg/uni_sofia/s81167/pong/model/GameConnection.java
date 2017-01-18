package bg.uni_sofia.s81167.pong.model;

import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import bg.uni_sofia.s81167.pong.game.Command;


public class GameConnection {
	private Socket socket;
	private Queue<Command> commandQueue;
	
	public GameConnection(){
	}
	
	public boolean isActive(){
		return socket != null;
	}
	
	public void deactivate(){
		socket = null;
		commandQueue = null;
	}
	
	public void activate(Socket socket){
		if(this.socket == null){
			this.socket = socket;
			this.commandQueue = new LinkedList<>();
		}
	}
	
	public Queue<Command> getCommandQueue(){
		return commandQueue;
	}

	public Socket getSocket() {
		return socket;
	}
	
}

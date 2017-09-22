import java.io.*;
import java.net.*;

public class PeerDescriptor implements Serializable{
	
	private static final long serialVersionUID = 1L;
	public int id;
	public String host;
	public int port;
	public String nickname;
	
	public PeerDescriptor(int id, String host, int port, String nickname) {
		this.id = id;
		this.host = host;
		this.port = port;
		this.nickname = nickname;
	}
	
	public PeerDescriptor() {
		
	}
	
	public String toString() {
		return "["+id +", "+host+", "+port+", "+nickname+"]";
	}

}

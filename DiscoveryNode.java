import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscoveryNode extends Thread {
	 
	//ThreadPool defines the maximum number of threads that can run at once. 	 
	ExecutorService threadPool = Executors.newFixedThreadPool(10);
	static ArrayList<PeerDescriptor> peerList = new ArrayList<PeerDescriptor>();
	boolean RUNNING = true;	
	int port;
	
	/**
	 * Start the DiscoveryNode
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {		
		DiscoveryNode s = new DiscoveryNode();
		s.run();
	}

	/**
	 * Constructor that reads the hostname and port from a config file
	 */
	public DiscoveryNode() {
		// System.out.println("server");
		File f = new File("discovery_node.txt");
        Scanner scanner =null;
		try {
			scanner = new Scanner(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        if(scanner.hasNext()){
        	String[] str = scanner.nextLine().split(":");
        	this.port = Integer.parseInt(str[1]);
        }
	}

	/** 
	 * Whenever a connection is established using socket create a DiscoveryNodeThread and put the thread into the thread execution pool.
	 */
	public void run() {
		try {
			ServerSocket svsocket = new ServerSocket(this.port);
			while (RUNNING) {
				Socket sock = svsocket.accept();
				this.threadPool.execute(new DiscoveryNodeThread(sock));
			}
			svsocket.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		this.threadPool.shutdown();
	}
}

class DiscoveryNodeThread implements Runnable {
	Socket sock;

	/**
	 * Constructor
	 * 
	 * @param sock	A socket connection accepted by the server socket.
	 */
	public DiscoveryNodeThread(Socket sock) {
		this.sock = sock;
	}

	
	/**
	 * Read a message indicating the action to be taken and take that action.
	 */
	public void run() {
		try {
			String message = ChordUtils.readStringFromSocket(sock);
			System.out.println(message);
			if(message.equals("#GET_RANDOM#")) {
				int size = DiscoveryNode.peerList.size();
				if(size > 0) {
					ChordUtils.writeStringToSocket(sock, "#OK#");
					synchronized (DiscoveryNodeThread.class) {
						ChordUtils.writeObjectToSocket(sock, DiscoveryNode.peerList.get(new Random().nextInt(size)));
					}
				}else {
					ChordUtils.writeStringToSocket(sock, "#EMPTY#");
				}
				
			}
			else if (message.equals("#ADD_PEER#")) {
				PeerDescriptor peerDesc = (PeerDescriptor) ChordUtils.readObjectFromSocket(sock);
				System.out.println(peerDesc);
				synchronized (DiscoveryNodeThread.class) {
					boolean collision = false;
					for (PeerDescriptor n : DiscoveryNode.peerList) {
						if (n.id == peerDesc.id) {
							collision = true;
						}
					}
					if (!collision) {
						// Return a random node from the list of nodes
						ChordUtils.writeStringToSocket(sock, "#OK#");
						int size = DiscoveryNode.peerList.size();
						if(size==0) {
							ChordUtils.writeObjectToSocket(sock, peerDesc);
						}else {
							ChordUtils.writeObjectToSocket(sock, DiscoveryNode.peerList.get(new Random().nextInt(size)));
						}
						// add the new peer to the list of peers 
						DiscoveryNode.peerList.add(peerDesc);
					}else {
						ChordUtils.writeStringToSocket(sock, "#COLLISION#");
					}
				}
			
			}else if(message.equals("#REMOVE_PEER#")) {
				int peerID =  Integer.parseInt(ChordUtils.readStringFromSocket(sock));
				System.out.println(peerID);
				synchronized (DiscoveryNodeThread.class) {
					for(int i = 0; i < DiscoveryNode.peerList.size(); i++) {
						if(DiscoveryNode.peerList.get(i).id == peerID) {
							DiscoveryNode.peerList.remove(i);
							break;
						}
					}					
				}
				//Tell the peer that it is safe to terminate now (graceful termination)
				//Socket sock = new Socket(peerDesc.host, peerDesc.port);
				ChordUtils.writeStringToSocket(sock, "#TERMINATE#");				
			}
			sock.close();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
}

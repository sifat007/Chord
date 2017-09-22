import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscoveryNode extends Thread {

	ExecutorService threadPool = Executors.newFixedThreadPool(10);

	static ArrayList<PeerDescriptor> peerList = new ArrayList<PeerDescriptor>();

	boolean RUNNING = true;
	
	int port;

	public static void main(String[] args) throws Exception {		
		DiscoveryNode s = new DiscoveryNode();
		s.run();
	}

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

	public DiscoveryNodeThread(Socket sock) {
		this.sock = sock;
	}

	public void run() {

		try {
			String message = ChordUtils.readStringFromSocket(sock);
			System.out.println(message);
			if (message.equals("#ADD_PEER#")) {
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
				PeerDescriptor peerDesc = (PeerDescriptor) ChordUtils.readObjectFromSocket(sock);
				synchronized (DiscoveryNodeThread.class) {
					for(int i = 0; i < DiscoveryNode.peerList.size(); i++) {
						if(DiscoveryNode.peerList.get(i).id == peerDesc.id) {
							DiscoveryNode.peerList.remove(i);
							break;
						}
					}					
				}
			}

		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
}

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Peer extends Thread{
	
    ExecutorService threadPool = Executors.newFixedThreadPool(10);
    boolean RUNNING = true;
    FingerTable fingerTable;
    int peerID;
    int port;
    String nickname;
    String hostname;
    
    String DISCOVERY_HOSTNAME;
    int DISCOVERY_PORT;

    //Usage: java Peer <port> [id] [nickname]
	public static void main(String[] args) {
		int id = 0;
		int port = 0;
		String nickname;
		if(args.length>0) {
			port = Integer.parseInt(args[0]);
		}else {
			System.out.println("Usage: java Peer <port> [id] [nickname]");
			return;
		}
		if(args.length>2) {
			nickname = args[2];
		}else {
			nickname = null;
		}
		if(args.length>1) {
			id = Integer.parseInt(args[1]);
		}else {
			long time = new Date().getTime();
			id = ChordUtils.CRC16((time + "").getBytes());
		}
				
		Peer p = new Peer(id,port,nickname);
		p.register();
		p.run();
	}
	
	public Peer(int id,int port, String nickname) {
		this.port = port;
		this.peerID = id;		
		try {
			this.hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if(nickname != null) {
			this.nickname = nickname;
		}else {
			this.nickname = hostname + ":" + this.port;
		}
		this.fingerTable = new FingerTable(this.peerID,this.hostname,this.port,this.nickname);
	}
	
	public void register() {
		//TODO: contact discovery node
		// Read disovery node info from a file
        File f = new File("discovery_node.txt");
        Scanner scanner =null;
		try {
			scanner = new Scanner(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        if(scanner.hasNext()){
        	String[] str = scanner.nextLine().split(":");
        	DISCOVERY_HOSTNAME = str[0];
        	DISCOVERY_PORT = Integer.parseInt(str[1]);
        }
        // Connect to the discovery node
        try {
        	boolean done = false;
        	while(!done) {
        		Socket sock = new Socket(DISCOVERY_HOSTNAME,DISCOVERY_PORT);
    			ChordUtils.writeStringToSocket(sock, "#ADD_PEER#");
    			ChordUtils.writeObjectToSocket(sock, new PeerDescriptor(this.peerID,this.hostname,this.port,this.nickname));
    			String message = ChordUtils.readStringFromSocket(sock);
    			System.out.println(message);
    			if(message.equals("#OK#")) {
    				done = true;
    				PeerDescriptor randomPeer = (PeerDescriptor)ChordUtils.readObjectFromSocket(sock);
    				System.out.println(randomPeer);
    				if(randomPeer.id == this.peerID) {
    					//Do nothing
    				}else {  // Returned a new node
    					
    				}
    			}else if(message.equals("#COLLISION#")) {
    				
    			}
    			sock.close();
        	}
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
        
	}
	
	@Override
	public void run() {
		try{
            ServerSocket svsocket = new ServerSocket(this.port);   
            while(RUNNING){
                Socket sock = svsocket.accept();
                this.threadPool.execute(new PeerThread(sock));
            }
            svsocket.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        this.threadPool.shutdown();
		
	}
}


class PeerThread implements Runnable {
	Socket sock;

	public PeerThread(Socket sock) {
		this.sock = sock;
	}

	public void run() {

		try {
			ObjectInputStream OIS = new ObjectInputStream(this.sock.getInputStream());
			PeerDescriptor nodeDesc = (PeerDescriptor) OIS.readObject();
			synchronized (PeerThread.class) {
				boolean collision = false;
				for (PeerDescriptor n : DiscoveryNode.peerList) {
					if (n.id == nodeDesc.id) {
						// TODO: ID Collision; request new ID
						collision = true;
					}
				}
				if (!collision) {
					DiscoveryNode.peerList.add(nodeDesc);
					// TODO: Return a random node from the list of nodes
				}
			}

		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		String message = null;
		// String message = BR.readLine();
		if (message.startsWith("#JOIN#")) {
			// System.out.println("Server: Reporting to Collator " +
			// DiscoveryNode.COLLATOR_HOST +":"+DiscoveryNode.COLLATOR_PORT);
			// Socket sock1 = new
			// Socket(DiscoveryNode.COLLATOR_HOST,DiscoveryNode.COLLATOR_PORT);
			// PrintStream PS = new PrintStream(sock1.getOutputStream());
			// PS.println("#SERVER_REPORT#"+DiscoveryNode.HOSTNAME
			// +":"+DiscoveryNode.PORT_NO + ","+ DiscoveryNode.receivedMessages + "," +
			// DiscoveryNode.receiveSummation);
			// sock1.close();
			return;
		}

	}
}

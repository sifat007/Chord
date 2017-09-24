import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Peer extends Thread{
	
    ExecutorService threadPool = Executors.newFixedThreadPool(10);
    boolean RUNNING = true;
    FingerTable fingerTable;
    PeerDescriptor predecessor;
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
		//Create a thread that can in close the Peer gracefully
		
		p.start();
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
    					//First peer
    					this.predecessor = randomPeer;
    				}else {  // Returned a new node; create finger table
    					try {    						
    						//fill up my finger table
    						for(int i = 0 ; i < fingerTable.size(); i++) {
    							Socket sock1 = new Socket(randomPeer.host, randomPeer.port);
    							ChordUtils.writeStringToSocket(sock1, "#LOOKUP#");
        						ChordUtils.writeStringToSocket(sock1, ""+(int)(this.peerID + Math.pow(2, i)));
        						PeerDescriptor pd = (PeerDescriptor)ChordUtils.readObjectFromSocket(sock1);
        						fingerTable.setPeerDescriptor(i, pd);
    						}
    						//find my successor
    						Socket sock1 = new Socket(randomPeer.host, randomPeer.port);
    						ChordUtils.writeStringToSocket(sock1, "#LOOKUP#");
    						ChordUtils.writeStringToSocket(sock1, ""+this.peerID);
    						PeerDescriptor succ = (PeerDescriptor)ChordUtils.readObjectFromSocket(sock1);
    						sock1.close();
    						//update predecessor of my successor
    						Socket sock2 = new Socket(succ.host, succ.port);
    						ChordUtils.writeStringToSocket(sock2, "#UPDATE_PRED#");
    						// Previous predecessor of the successor becomes by predecessor
    						this.predecessor = (PeerDescriptor)ChordUtils.readObjectFromSocket(sock2);
    						ChordUtils.writeObjectToSocket(sock2, new PeerDescriptor(this.peerID,this.hostname,this.port,this.nickname));    						
    						sock2.close();
    					} catch (IOException e) {
    						e.printStackTrace();
    					} 					
    				}
    			}else if(message.equals("#COLLISION#")) {
    				System.out.println("Peer ID Collision. Please enter a new ID(0-65536):");
    				Scanner scan = new Scanner(System.in);
    				String str = scan.next();
    				int new_id;
    				try {
    					new_id = Integer.parseInt(str);
    				}catch(NumberFormatException e) {
    					System.out.print("Generating a random peer ID.");
    					long time = new Date().getTime();
    					new_id = ChordUtils.CRC16((time + "").getBytes());
    				}
    				this.peerID = new_id; 				
    			}
    			sock.close();
        	}
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}        
	}
	
	
	public PeerDescriptor lookup(int k) throws UnknownHostException, IOException {
		if(ChordUtils.isInBetween(this.predecessor.id, k, this.peerID)) {
			return new PeerDescriptor(this.peerID,this.hostname,this.port,this.nickname);
		}
		PeerDescriptor pd = this.fingerTable.nextHop(k, this.peerID);
		Socket sock = new Socket(pd.host, pd.port);
		ChordUtils.writeStringToSocket(sock, "#LOOKUP#");
		ChordUtils.writeObjectToSocket(sock, this.peerID + "");
		try {
			PeerDescriptor succ = (PeerDescriptor)ChordUtils.readObjectFromSocket(sock);
			sock.close();
			return succ;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			sock.close();
			return null;
		} 
		
	}
	
	@Override
	public void run() {
		new IOThread(this).start();
		try{
            ServerSocket svsocket = new ServerSocket(this.port);   
            while(RUNNING){
                Socket sock = svsocket.accept();
                this.threadPool.execute(new PeerThread(sock,this));
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
	Peer peer;

	public PeerThread(Socket sock, Peer peer) {
		this.sock = sock;
		this.peer = peer;
	}

	public void run() {

		try {
			String message = ChordUtils.readStringFromSocket(sock);
			System.out.println(message);
			if(message.equals("#TERMINATE#")) {
				//TODO: update other finger tables				
			}else if(message.equals("#LOOKUP#")) {
				String id = ChordUtils.readStringFromSocket(sock);
				PeerDescriptor pd = peer.lookup(Integer.parseInt(id));
				ChordUtils.writeObjectToSocket(sock, pd);
			}else if(message.equals("#UPDATE_PRED#")) {
				ChordUtils.writeObjectToSocket(sock, peer.predecessor);
				PeerDescriptor pd = (PeerDescriptor)ChordUtils.readObjectFromSocket(sock);				
				peer.predecessor = pd;
				//send relevant files to pd
				
			}
			sock.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		

	}
}



class IOThread extends Thread{
	Peer peer;
	public IOThread(Peer peer) {
		this.peer = peer;
	}
	@Override
	public void run() {
		Scanner scan = new Scanner(System.in);
		while(scan.hasNext()) {
			String str = scan.next();
			if(str.equals("exit")) {				
				try {
					Socket sock = new Socket(peer.DISCOVERY_HOSTNAME,peer.DISCOVERY_PORT);
					ChordUtils.writeStringToSocket(sock, "#REMOVE_PEER#");
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}		    			
			}
			if(str.equals("print")) {
				System.out.println("Predecessor: " + peer.predecessor);
				System.out.println(peer.fingerTable);				
			}
		}
	}
}
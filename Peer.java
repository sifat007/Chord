import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Peer extends Thread {

	ExecutorService threadPool = Executors.newFixedThreadPool(20);
	static boolean RUNNING = true;
	FingerTable fingerTable;
	PeerDescriptor predecessor;
	int peerID;
	int port;
	String nickname;
	String hostname;

	String DISCOVERY_HOSTNAME;
	int DISCOVERY_PORT;

	Storage storage;

	// Usage: java Peer <port> [id] [nickname]
	public static void main(String[] args) {
		int id = 0;
		int port = 0;
		String nickname;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			System.out.println("Usage: java Peer <port> [id] [nickname]");
			return;
		}
		if (args.length > 2) {
			nickname = args[2];
		} else {
			nickname = null;
		}
		if (args.length > 1) {
			id = Integer.parseInt(args[1], 16);
			if (id >= Math.pow(2, 16)) {
				System.out.println("Specified ID is too large");
				return;
			}
		} else {
			long time = new Date().getTime();
			id = ChordUtils.CRC16((time + "").getBytes());
		}

		Peer p = new Peer(id, port, nickname);

		p.start();
		p.register();
		p.startOtherThreads();

	}

	public Peer(int id, int port, String nickname) {
		this.port = port;
		this.peerID = id;
		try {
			this.hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if (nickname != null) {
			this.nickname = nickname;
		} else {
			this.nickname = hostname + ":" + this.port;
		}
		this.fingerTable = new FingerTable(this.peerID, this.hostname, this.port, this.nickname);
		this.storage = new Storage(this);

		// Read discovery node info from a file
		File f = new File("discovery_node.txt");
		Scanner scanner = null;
		try {
			scanner = new Scanner(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (scanner.hasNext()) {
			String[] str = scanner.nextLine().split(":");
			DISCOVERY_HOSTNAME = str[0];
			DISCOVERY_PORT = Integer.parseInt(str[1]);
		}

	}

	public void startOtherThreads() {
		new IOThread(this).start();
		new HeartbeatThread(this).start();
	}

	public void register() {

		// Connect to the discovery node
		try {
			boolean done = false;
			while (!done) {
				Socket sock = new Socket(DISCOVERY_HOSTNAME, DISCOVERY_PORT);
				ChordUtils.writeStringToSocket(sock, "#ADD_PEER#");
				ChordUtils.writeObjectToSocket(sock,
						new PeerDescriptor(this.peerID, this.hostname, this.port, this.nickname));
				String message = ChordUtils.readStringFromSocket(sock);
				System.out.println(message);
				if (message.equals("#OK#")) {
					done = true;
					PeerDescriptor randomPeer = (PeerDescriptor) ChordUtils.readObjectFromSocket(sock);
					System.out.println(randomPeer);
					if (randomPeer.id == this.peerID) {
						// First peer
						this.predecessor = randomPeer;
					} else { // Returned a new node; create finger table
						try {
							// find my successor
							Socket sock1 = new Socket(randomPeer.host, randomPeer.port);
							ChordUtils.writeStringToSocket(sock1, "#LOOKUP#");
							ChordUtils.writeStringToSocket(sock1, "" + this.peerID);
							PeerDescriptor succ = (PeerDescriptor) ChordUtils.readObjectFromSocket(sock1);
							this.fingerTable.setPeerDescriptor(0, succ);
							sock1.close();
							// update predecessor of my successor
							Socket sock2 = new Socket(succ.host, succ.port);
							ChordUtils.writeStringToSocket(sock2, "#UPDATE_PRED#");
							// Previous predecessor of the successor becomes my predecessor
							this.predecessor = (PeerDescriptor) ChordUtils.readObjectFromSocket(sock2);
							ChordUtils.writeObjectToSocket(sock2,
									new PeerDescriptor(this.peerID, this.hostname, this.port, this.nickname));
							sock2.close();
							// Update successor of my predecessor
							Socket sock3 = new Socket(this.predecessor.host, this.predecessor.port);
							ChordUtils.writeStringToSocket(sock3, "#UPDATE_SUCC#");
							ChordUtils.writeObjectToSocket(sock3,
									new PeerDescriptor(this.peerID, this.hostname, this.port, this.nickname));
							sock3.close();
							System.out.println("here");
							// fill up my finger table; initialization
							for (int i = 1; i < fingerTable.size(); i++) {
								int curr = (int) (Math.pow(2, i));
								int max = (int) (Math.pow(2, 16));
								PeerDescriptor pd = this.lookup((this.peerID + curr) % max);
								synchronized (HeartbeatThread.class) {
									this.fingerTable.setPeerDescriptor(i, pd);
								}
							}
							// TODO: Request relevant files for my successor
							Socket sock4 = new Socket(succ.host, succ.port);
							ChordUtils.writeStringToSocket(sock4, "#SEND_FILES#");
							sock4.close();

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else if (message.equals("#COLLISION#")) {
					System.out.println("Peer ID Collision. Please enter a new ID(0-65536):");
					Scanner scan = new Scanner(System.in);
					String str = scan.next();
					int new_id;
					try {
						new_id = Integer.parseInt(str);
					} catch (NumberFormatException e) {
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
		// System.out.println("ChorUtils.isInBetween(" +this.predecessor.id+ "," + k +
		// ","+ this.peerID + ")" + " = "+ ChordUtils.isInBetween(this.predecessor.id,
		// k, this.peerID));
		if (ChordUtils.isInBetween(this.predecessor.id, k, this.peerID) || k == this.peerID) {
			return new PeerDescriptor(this.peerID, this.hostname, this.port, this.nickname);
		}
		PeerDescriptor pd = this.fingerTable.nextHop(k);
		Socket sock = null;
		try {
			sock = new Socket(pd.host, pd.port);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// fault-tolerance
		if (sock == null) { // if the lookup fails; the peer may be down and finger-table is not yet
							// updated; go to your successor
			PeerDescriptor succ = this.fingerTable.get(0);
			sock = new Socket(succ.host, succ.port);
		}
		ChordUtils.writeStringToSocket(sock, "#LOOKUP#");
		ChordUtils.writeObjectToSocket(sock, k + "");
		try {
			PeerDescriptor succ = (PeerDescriptor) ChordUtils.readObjectFromSocket(sock);
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

		try {
			ServerSocket svsocket = new ServerSocket(this.port);
			while (RUNNING) {
				Socket sock = svsocket.accept();
				this.threadPool.execute(new PeerThread(sock, this));
			}
			svsocket.close();
		} catch (Exception ex) {
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
			// System.out.println(message);
			if (message.equals("#LOOKUP#")) {
				String id = ChordUtils.readStringFromSocket(sock);
				PeerDescriptor pd = peer.lookup(Integer.parseInt(id));
				ChordUtils.writeObjectToSocket(sock, pd);
			} else if (message.equals("#UPDATE_PRED#")) {
				ChordUtils.writeObjectToSocket(sock, peer.predecessor);
				PeerDescriptor pd = (PeerDescriptor) ChordUtils.readObjectFromSocket(sock);
				synchronized (PeerThread.class) {
					peer.predecessor = pd;
				}
				// send relevant files to pd
			} else if (message.equals("#UPDATE_SUCC#")) {
				PeerDescriptor pd = (PeerDescriptor) ChordUtils.readObjectFromSocket(sock);
				synchronized (PeerThread.class) {
					peer.fingerTable.setPeerDescriptor(0, pd);
				}
			} else if (message.equals("#STORE_DATA#")) {
				String id = ChordUtils.readStringFromSocket(sock);
				String filename = ChordUtils.readFileFromSocket(sock);
				synchronized (PeerThread.class) {
					peer.storage.store(Integer.parseInt(id), filename);
				}
				// System.out.println(path);
			} else if (message.equals("#SEND_FILES#")) {
				// Send relevant files to predecessor
				sendFilesToPredecessor();
				
				

			}
			sock.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	private synchronized void sendFilesToPredecessor() throws UnknownHostException, IOException {
		Set<Integer> keys = peer.storage.getAllKeys();
		ArrayList<Integer> d_list = new ArrayList<Integer>();
		for (int k : keys) {
			if (ChordUtils.isInBetween(peer.predecessor.id, k, peer.peerID) == false && k != peer.peerID) {
				Socket sock4 = new Socket(peer.predecessor.host, peer.predecessor.port);
				ChordUtils.writeStringToSocket(sock4, "#STORE_DATA#");
				ChordUtils.writeStringToSocket(sock4, "" + k);
				ChordUtils.writeFileToSocket(sock4, "/tmp/" + peer.storage.get(k));
				sock4.close();
				d_list.add(k);
			}
		}
		for(int i : d_list) {
			peer.storage.remove(i);
		}
		
	}
}

class HeartbeatThread extends Thread {
	Peer peer;

	public HeartbeatThread(Peer peer) {
		this.peer = peer;
	}

	@Override
	public void run() {
		while (Peer.RUNNING) {
			System.out.println("Heartbeat");
			for (int i = 1; i < peer.fingerTable.size(); i++) {
				try {
					int curr = (int) (Math.pow(2, i));
					int max = (int) (Math.pow(2, 16));
					PeerDescriptor pd = peer.lookup((peer.peerID + curr) % max);
					synchronized (HeartbeatThread.class) {
						peer.fingerTable.setPeerDescriptor(i, pd);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

	}
}

class IOThread extends Thread {
	Peer peer;

	public IOThread(Peer peer) {
		this.peer = peer;
	}

	@Override
	public void run() {
		Scanner scan = new Scanner(System.in);
		while (scan.hasNext()) {
			String str = scan.next();
			if (str.equals("exit")) {
				try {
					Socket sock = new Socket(peer.DISCOVERY_HOSTNAME, peer.DISCOVERY_PORT);
					ChordUtils.writeStringToSocket(sock, "#REMOVE_PEER#");
					ChordUtils.writeStringToSocket(sock, peer.peerID + "");
					String message = ChordUtils.readStringFromSocket(sock);
					sock.close();
					System.out.println(message);
					if (message.equals("#TERMINATE#")) {
						// TODO: remove myself from the ring
						// update predecessor of my successor
						PeerDescriptor succ = peer.fingerTable.get(0);
						if (succ.id == peer.peerID) {
							// If I am the last node; just get out
							break;
						}
						Socket sock2 = new Socket(succ.host, succ.port);
						ChordUtils.writeStringToSocket(sock2, "#UPDATE_PRED#");
						ChordUtils.readObjectFromSocket(sock2); // read the previous predecessor; we are overwriting it.
						// my predecessor becomes the predecessor of my successor
						ChordUtils.writeObjectToSocket(sock2, peer.predecessor);
						sock2.close();
						// Update successor of my predecessor
						Socket sock3 = new Socket(peer.predecessor.host, peer.predecessor.port);
						ChordUtils.writeStringToSocket(sock3, "#UPDATE_SUCC#");
						// my successor become the successor of my predecessor
						ChordUtils.writeObjectToSocket(sock3, succ);
						sock3.close();
						// TODO: send all my files to my successor
						Set<Integer> keys = peer.storage.getAllKeys();
						for (int k : keys) {
							Socket sock4 = new Socket(succ.host, succ.port);
							ChordUtils.writeStringToSocket(sock4, "#STORE_DATA#");
							ChordUtils.writeStringToSocket(sock4, "" + k);
							ChordUtils.writeFileToSocket(sock4, "/tmp/" + peer.storage.get(k));
							sock4.close();
						}

						Peer.RUNNING = false;
					}

				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else if (str.equals("print")) {
				System.out.println("Peer ID:" + Integer.toHexString(peer.peerID));
				System.out.println("Predecessor: " + peer.predecessor);
				System.out.println(peer.fingerTable);
				System.out.println(peer.storage);
			} else if (str.equals("lookup")) {
				int i = scan.nextInt();
				try {
					System.out.println(peer.lookup(i));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
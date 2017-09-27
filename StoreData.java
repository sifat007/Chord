
import java.io.*;
import java.net.*;
import java.util.*;

public class StoreData {
	
	File file;
    String DISCOVERY_HOSTNAME;
    int DISCOVERY_PORT;
    PeerDescriptor peer;
    int k;
    
    public static void main(String[] args) {
    	String file_path = null;
    	int key = -1;
    	if(args.length>0) {
			file_path = args[0];
		}else {
			System.out.println("Usage: java StoreData <filepath> [id]");
			return;
		}
    	if(args.length > 1) {
    		key = Integer.parseInt(args[1],10);
    		if(key >= Math.pow(2, 16)) {
				System.out.println("Specified ID is too large");
				return;
			}
    	}
    	
    	StoreData s = new StoreData(file_path, key);
    	s.store();

	}
    
    public StoreData(String file_path, int key) {
    	this.file = new File(file_path);
    	
		// Read discovery node info from a file
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
        if(key == -1) {
        	this.k = ChordUtils.CRC16(this.file.getName().getBytes());
        }else {
        	this.k = key;
        }
        
	}
    
    public void store() {
    	try {
			Socket sock = new Socket(DISCOVERY_HOSTNAME,DISCOVERY_PORT);	
			ChordUtils.writeStringToSocket(sock, "#GET_RANDOM#");
			String message = ChordUtils.readStringFromSocket(sock);
			if(message.equals("#OK#")) {
				try {
					PeerDescriptor randomPeer = (PeerDescriptor)ChordUtils.readObjectFromSocket(sock);
					Socket sock1 = new Socket(randomPeer.host, randomPeer.port);
					ChordUtils.writeStringToSocket(sock1, "#LOOKUP#");					
					ChordUtils.writeStringToSocket(sock1, ""+this.k); //id/key
					ChordUtils.writeStringToSocket(sock1, ""+this.k); //fromID
					ChordUtils.writeStringToSocket(sock1, false + "");//isHeartbeat
					this.peer = (PeerDescriptor)ChordUtils.readObjectFromSocket(sock1);
					sock1.close();
					// Send data to peer
					System.out.println(this.peer);
					Socket sock2 = new Socket(this.peer.host, this.peer.port);
					ChordUtils.writeStringToSocket(sock2, "#STORE_DATA#");
					ChordUtils.writeStringToSocket(sock2, ""+this.k);
					ChordUtils.writeFileToSocket(sock2, this.file.getPath());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}else {
				System.out.println("No Peers in the system.");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}

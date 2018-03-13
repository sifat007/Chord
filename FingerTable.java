
/**
 * Data structure for the Finger Table of a Peer
 * 
 * @author Tarequl Islam Sifat
 *
 */
public class FingerTable {
	
	// Finger table will contain 16 peer descriptors
	private PeerDescriptor[] list = new PeerDescriptor[16];
	int peerID;
	
	/**
	 * Constructor 
	 * 
	 * @param id Peer ID
	 * @param hostname
	 * @param post
	 * @param nickname A nickname for the peer
	 */
	public FingerTable(int id, String hostname, int post, String nickname) {
		this.peerID = id;
		for(int i = 0 ; i < 16; i++) {
			list[i] = new PeerDescriptor(id,hostname,post,nickname);			
		}
	}
	
	/**
	 * Size of the Finger Table
	 */
	public synchronized int size() {
		return list.length;				
	}
	
	/**
	 * Get a specific PeerDescriptor
	 * 
	 * @param index
	 * @return
	 */
	public synchronized PeerDescriptor get(int index) {
		return list[index];
	}
	
	/**
	 * Set a specific PeerDescriptor
	 * 
	 * @param index
	 * @param pDesc
	 */
	public synchronized void setPeerDescriptor(int index, PeerDescriptor pDesc) {
		this.list[index] = pDesc;
	}
	
	/**
	 * Calculate the next hop to eventually reach a hop in the network that is responsible for the ID 'k'
	 * 
	 * @param k The ID for which are looking for the responsible Peer in the network
	 * @return
	 */
	public synchronized PeerDescriptor nextHop(int k) {
		if(ChordUtils.isInBetween(peerID, k, list[0].id) || peerID == list[0].id) {
			return list[0];
		}
		for(int i = 0; i < list.length - 1; i++) {
			if(list[i].id == k || ChordUtils.isInBetween(list[i].id, k, list[i+1].id)) {
				return list[i];
			}
		}
		return list[list.length - 1];
	}
	
	/** 
	 * A presentable string for the Finger Table
	 */
	@Override
	public synchronized String toString() {
		String  s= "Finger Table\n===================\n";
		int i = 1;		
		for(PeerDescriptor p:list) {
			s+= (i + "|" + p + "\n");
			i++;
		}
		s += "===================\n";
		return s;
	}

}



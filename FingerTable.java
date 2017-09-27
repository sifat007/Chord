
public class FingerTable {
	
	private PeerDescriptor[] list = new PeerDescriptor[16];
	int peerID;
	public FingerTable(int id, String hostname, int post, String nickname) {
		this.peerID = id;
		for(int i = 0 ; i < 16; i++) {
			list[i] = new PeerDescriptor(id,hostname,post,nickname);			
		}
	}
	
	public synchronized int size() {
		return list.length;				
	}
	
	public synchronized PeerDescriptor get(int index) {
		return list[index];
	}
	
	public synchronized void setPeerDescriptor(int index, PeerDescriptor pDesc) {
		this.list[index] = pDesc;
	}
	
	public synchronized PeerDescriptor nextHop(int k) {
		//if(k < peerID) k += Math.pow(2, 16);
		//if(peerID < k && k < list[0].id + ((list[0].id<peerID)?(int)Math.pow(2, 16):0)) {
		//System.out.println("ChorUtils.isInBetween(" +peerID+ "," + k + ","+ list[0].id + ")" + " = "+ ChordUtils.isInBetween(peerID, k, list[0].id));
		if(ChordUtils.isInBetween(peerID, k, list[0].id) || peerID == list[0].id) {
			return list[0];
		}
		for(int i = 0; i < list.length - 1; i++) {
			//if(list[i].id + ((list[i].id<peerID)?(int)Math.pow(2, 16):0) <= k && k < list[i+1].id + ((list[i+1].id<peerID)?(int)Math.pow(2, 16):0)) {
			//System.out.println(i + " | " + "ChorUtils.isInBetween(" +list[i].id+ "," + k + ","+ list[i+1].id + ")" + " = "+ ChordUtils.isInBetween(list[i].id, k, list[i+1].id));
			if(list[i].id == k || ChordUtils.isInBetween(list[i].id, k, list[i+1].id)) {
				return list[i];
			}
		}
		return list[list.length - 1];
	}
	
	public synchronized PeerDescriptor nextHop1(int k) {
		//if(k < peerID) k += Math.pow(2, 16);
		//if(peerID < k && k < list[0].id + ((list[0].id<peerID)?(int)Math.pow(2, 16):0)) {
		System.out.println("ChorUtils.isInBetween(" +peerID+ "," + k + ","+ list[0].id + ")" + " = "+ ChordUtils.isInBetween(peerID, k, list[0].id));
		if(ChordUtils.isInBetween(peerID, k, list[0].id) || peerID == list[0].id) {
			return list[0];
		}
		for(int i = 0; i < list.length - 1; i++) {
			//if(list[i].id + ((list[i].id<peerID)?(int)Math.pow(2, 16):0) <= k && k < list[i+1].id + ((list[i+1].id<peerID)?(int)Math.pow(2, 16):0)) {
			System.out.println(i + " | " + "ChorUtils.isInBetween(" +list[i].id+ "," + k + ","+ list[i+1].id + ")" + " = "+ ChordUtils.isInBetween(list[i].id, k, list[i+1].id));
			if(list[i].id == k || ChordUtils.isInBetween(list[i].id, k, list[i+1].id)) {
				return list[i];
			}
		}
		return list[list.length - 1];
	}
	
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



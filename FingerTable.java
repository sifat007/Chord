
public class FingerTable {
	
	private PeerDescriptor[] list = new PeerDescriptor[16];
	
	public FingerTable(int id, String hostname, int post, String nickname) {
		for(int i = 0 ; i < 16; i++) {
			list[i] = new PeerDescriptor(id,hostname,post,nickname);			
		}
	}
	
	public int size() {
		return list.length;				
	}
	
	public void setPeerDescriptor(int index, PeerDescriptor pDesc) {
		this.list[index] = pDesc;
	}
	
	public PeerDescriptor nextHop(int k, int peerID) {
		//if(k < peerID) k += Math.pow(2, 16);
		//if(peerID < k && k < list[0].id + ((list[0].id<peerID)?(int)Math.pow(2, 16):0)) {
		if(ChordUtils.isInBetween(peerID, k, list[0].id)) {
			return list[0];
		}
		for(int i = 1; i < list.length - 1; i++) {
			//if(list[i].id + ((list[i].id<peerID)?(int)Math.pow(2, 16):0) <= k && k < list[i+1].id + ((list[i+1].id<peerID)?(int)Math.pow(2, 16):0)) {
			if(list[i].id == k || ChordUtils.isInBetween(list[i].id, k, list[i+1].id)) {
				return list[i];
			}
		}
		return list[list.length - 1];
	}
	
	@Override
	public String toString() {
		String  s= "Finger Table\n===================\n";
		for(PeerDescriptor p:list) {
			s+= p + "\n";
		}
		s += "===================\n";
		return s;
	}

}



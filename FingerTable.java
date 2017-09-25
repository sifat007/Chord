
public class FingerTable {
	
	private PeerDescriptor[] list = new PeerDescriptor[16];
	int peerID;
	public FingerTable(int id, String hostname, int post, String nickname, int peerID) {
		this.peerID = peerID;
		for(int i = 0 ; i < 16; i++) {
			list[i] = new PeerDescriptor(id,hostname,post,nickname);			
		}
	}
	
	public int size() {
		return list.length;				
	}
	
	public PeerDescriptor get(int index) {
		return list[index];
	}
	
	public void setPeerDescriptor(int index, PeerDescriptor pDesc) {
		this.list[index] = pDesc;
	}
	
	public PeerDescriptor nextHop(int k) {
		//if(k < peerID) k += Math.pow(2, 16);
		//if(peerID < k && k < list[0].id + ((list[0].id<peerID)?(int)Math.pow(2, 16):0)) {
		//System.out.println("ChorUtils.isInBetween(" +peerID+ "," + k + ","+ list[0].id + ")" + " = "+ ChordUtils.isInBetween(peerID, k, list[0].id));
		if(ChordUtils.isInBetween(peerID, k, list[0].id)) {
			return list[0];
		}
		for(int i = 0; i < list.length - 1; i++) {
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
		int i = 0;		
		for(PeerDescriptor p:list) {
			int curr = (int)(Math.pow(2, i));
			int max = (int)(Math.pow(2, 16));
			s+= ( (curr+peerID)%max ) + "|" + p + "\n";
			i++;
		}
		s += "===================\n";
		return s;
	}

}




public class FingerTable {
	
	PeerDescriptor[] list = new PeerDescriptor[16];
	
	public FingerTable(int id, String hostname, int post, String nickname) {
		for(int i = 0 ; i < 16; i++) {
			list[i] = new PeerDescriptor(id,hostname,post,nickname);			
		}
	}
	
	public void setPeerDescriptor(int index, PeerDescriptor pDesc) {
		this.list[index] = pDesc;
	}

}



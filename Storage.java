
import java.io.*;
import java.net.*;
import java.util.*;

public class Storage {
	Peer peer;

	Hashtable<Integer, String> hashtable;

	public Storage(Peer peer) {
		this.peer = peer;
		this.hashtable = new Hashtable<Integer, String>();
	}
	
	public synchronized void store(int hash, String filename) {
		this.hashtable.put(hash, filename);
	}
	
	public synchronized void remove(int hash) {
		this.hashtable.remove(hash);
	}
	
	public synchronized String get(int hash) {
		return this.hashtable.get(hash);
	}
	
	public synchronized String[] getAll() {
		Set<Integer> keys = this.hashtable.keySet();
		String arr[] = new String[keys.size()];
		int index  =0;
		for(int k:keys) {
			arr[index] = get(k);
		}
		return arr;		
	}
	public synchronized Set<Integer> getAllKeys() {
		return this.hashtable.keySet();		
	}
	
	public synchronized String toString() {
		Set<Integer> keys = this.hashtable.keySet();
		String  s= "Storage\n===================\n";
		for(int k:keys) {
			s += Integer.toHexString(k) + "\t|\t" + get(k) + "\n";
		}
		s += "===================\n";
		return s;
	}

}

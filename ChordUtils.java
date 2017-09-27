import java.io.*;
import java.net.*;

public class ChordUtils {
	/**
	 * This method converts a set of bytes into a Hexadecimal representation.
	 *
	 * @param buf
	 * @return
	 */
	public static String convertBytesToHex(byte[] buf) {
		StringBuffer strBuf = new StringBuffer();
		for (int i = 0; i < buf.length; i++) {
			int byteValue = (int) buf[i] & 0xff;
			if (byteValue <= 15) {
				strBuf.append("0");
			}
			strBuf.append(Integer.toString(byteValue, 16));
		}
		return strBuf.toString();
	}

	/**
	 * This method converts a specified hexadecimal String into a set of bytes.
	 *
	 * @param hexString
	 * @return
	 */
	public static byte[] convertHexToBytes(String hexString) {
		int size = hexString.length();
		byte[] buf = new byte[size / 2];
		int j = 0;
		for (int i = 0; i < size; i++) {
			String a = hexString.substring(i, i + 2);
			int valA = Integer.parseInt(a, 16);
			i++;
			buf[j] = (byte) valA;
			j++;
		}
		return buf;
	}

	/**
	 * 
	 * Reads in a string s as a command-line argument, and prints out its 16-bit
	 * Cyclic Redundancy Check (CRC16). Uses a lookup table.
	 *
	 * Reference: http://www.gelato.unsw.edu.au/lxr/source/lib/crc16.c
	 * Source: http://introcs.cs.princeton.edu/java/61data/CRC16.java
	 *
	 * % java CRC16 123456789 CRC16 = bb3d
	 *
	 * Uses irreducible polynomial: 1 + x^2 + x^15 + x^16
	 *
	 *
	 */
	public static int CRC16(byte[] bytes) {

		int[] table = { 0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241, 0xC601, 0x06C0, 0x0780, 0xC741,
				0x0500, 0xC5C1, 0xC481, 0x0440, 0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40, 0x0A00,
				0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841, 0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1,
				0xDA81, 0x1A40, 0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41, 0x1400, 0xD4C1, 0xD581,
				0x1540, 0xD701, 0x17C0, 0x1680, 0xD641, 0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
				0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240, 0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501,
				0x35C0, 0x3480, 0xF441, 0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41, 0xFA01, 0x3AC0,
				0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840, 0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80,
				0xEA41, 0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40, 0xE401, 0x24C0, 0x2580, 0xE541,
				0x2700, 0xE7C1, 0xE681, 0x2640, 0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041, 0xA001,
				0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240, 0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0,
				0x6480, 0xA441, 0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41, 0xAA01, 0x6AC0, 0x6B80,
				0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840, 0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
				0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40, 0xB401, 0x74C0, 0x7580, 0xB541, 0x7700,
				0xB7C1, 0xB681, 0x7640, 0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041, 0x5000, 0x90C1,
				0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241, 0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481,
				0x5440, 0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40, 0x5A00, 0x9AC1, 0x9B81, 0x5B40,
				0x9901, 0x59C0, 0x5880, 0x9841, 0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40, 0x4E00,
				0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41, 0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0,
				0x4680, 0x8641, 0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040, };

		int crc = 0x0000;
		for (byte b : bytes) {
			crc = (crc >>> 8) ^ table[(crc ^ b) & 0xff];
		}

		return crc;
	}
	
	
	public static String readStringFromSocket(Socket sock) throws IOException {
		ObjectInputStream OIS = new ObjectInputStream(sock.getInputStream());
		try {
			return (String)OIS.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Object readObjectFromSocket(Socket sock) throws IOException, ClassNotFoundException {
		ObjectInputStream OIS = new ObjectInputStream(sock.getInputStream());
		return OIS.readObject();
	}
	
	public static void writeStringToSocket(Socket sock, String message) throws IOException {
		ObjectOutputStream OOS = new ObjectOutputStream(sock.getOutputStream());
		OOS.writeObject(new String(message));
	}
	
	public static void writeObjectToSocket(Socket sock, Object obj) throws IOException {
		ObjectOutputStream OOS = new ObjectOutputStream(sock.getOutputStream());
		OOS.writeObject(obj);
	}
	
	/**
	 * Reads a file from socket, saves in the /tmp directory, returns the filename
	 * @param sock
	 * @return filename
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	
	public static String readFileFromSocket(Socket sock) throws IOException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
		String filename = (String)ois.readObject();
		//filename = "somefile.jpg";
		File file = new File("/tmp/"+filename);
		FileOutputStream fos = new FileOutputStream(file);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		int buff_size = 10000;
		byte[] contents;
		System.out.println("Here");
		//System.out.println(ois.readLong());		
		long filesize = ois.readLong();
		System.out.println(filesize);
		long iterations = filesize/buff_size + ((filesize%buff_size>0)?1:0);
		System.out.println(iterations);
		for(int i = 0 ; i < iterations; i++) {
			contents = (byte[])ois.readObject();
			bos.write(contents,0,contents.length);
		}		
		bos.flush();
		bos.close();
		return filename;
	}
	
	public static void writeFileToSocket(Socket sock, String filePath) throws IOException{
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(fis);		
		ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
		byte[] contents;
		long fileLength = file.length();
		long current = 0;
		oos.writeObject(new String(file.getName()));
		oos.writeLong(new Long(fileLength));
		while(current!=fileLength) {
			int size = 10000;
			if(fileLength - current >= size) {
				current += size;
			}else { 
                size = (int)(fileLength - current); 
                current = fileLength;
            } 
            contents = new byte[size]; 
            System.out.println(current + " "+ fileLength);
            bis.read(contents, 0, size); 
            oos.writeObject(contents);
		}
        bis.close();
	}

	public static boolean isInBetween(int prev, int k, int peerID) {
		if(prev == peerID) return false;
		else if( (prev < peerID) && (prev <  k && k < peerID )  ) return true;
		else if( (prev > peerID) && (peerID > k || k > prev)) return true;
		else return false;
	}
	

}

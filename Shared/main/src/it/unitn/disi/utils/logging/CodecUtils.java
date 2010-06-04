package it.unitn.disi.utils.logging;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodecUtils {
	
	public static int append(int number, byte [] buffer, int offset) {
		encode(number, buffer, offset);
		return offset + (Integer.SIZE/Byte.SIZE);
	}
	
	public static int append(byte b, byte[] buffer, int offset) {
		buffer[offset] = b;
		return offset + 1;
	}
	
	public static int append(long num, byte [] buf, int offset) {
		encode(num, buf, offset);
		return offset + (Long.SIZE/Byte.SIZE);
	}
	
	public static byte [] encode(long num, byte[] buf, int offset) {
		// Ugly coding but fast.
		buf[offset + 7] = (byte) ((num & 0xff00000000000000L) >>> 56);
		buf[offset + 6] = (byte) ((num & 0x00ff000000000000L) >>> 48);
		buf[offset + 5] = (byte) ((num & 0x0000ff0000000000L) >>> 40);
		buf[offset + 4] = (byte) ((num & 0x000000ff00000000L) >>> 32);
		buf[offset + 3] = (byte) ((num & 0x00000000ff000000L) >>> 24);
		buf[offset + 2] = (byte) ((num & 0x0000000000ff0000L) >>> 16);
		buf[offset + 1] = (byte) ((num & 0x000000000000ff00L) >>> 8);
		buf[offset] = 	  (byte) ((num & 0x00000000000000ffL));

		return buf;
	}
	
	public static long decodeLong(byte[] buf, int offset) {
		long num = (long) (buf[offset] & 0xff);
		num |= ((long) (buf[offset + 1] & 0xff) << 8);
		num |= ((long) (buf[offset + 2] & 0xff) << 16);
		num |= ((long) (buf[offset + 3] & 0xff) << 24);
		num |= ((long) (buf[offset + 4] & 0xff) << 32);
		num |= ((long) (buf[offset + 5] & 0xff) << 40);
		num |= ((long) (buf[offset + 6] & 0xff) << 48);
		num |= ((long) (buf[offset + 7] & 0xff) << 56);
		
		return num;
	}
	
	public static byte [] encode(int num, byte[] buf, int offset) {
		buf[offset + 3] = (byte) ((num & 0xff000000) >>> 24);
		buf[offset + 2] = (byte) ((num & 0x00ff0000) >>> 16);
		buf[offset + 1] = (byte) ((num & 0x0000ff00) >>> 8);
		buf[offset] = (byte) ((num & 0x000000ff));
		
		return buf;
	}
	
	public static int decodeInt(byte [] buf, int offset) {
		int num = (int) (buf[offset] & 0xff);
		num |= ((int) (buf[offset + 1] & 0xff) << 8);
		num |= ((int) (buf[offset + 2] & 0xff) << 16);
		num |= ((int) (buf[offset + 3] & 0xff) << 24);
		return num;
	}
	
	public static byte [] encode(int num, byte [] buf) {
		return encode(num, buf, 0);
	}
	
	public static int decodeInt(byte [] buf) {
		return decodeInt(buf, 0);
	}
	
	
	public static List<Class <? extends Number>> mkROCollection(Class<? extends Number>...components){
		ArrayList<Class <? extends Number>> al = new ArrayList<Class <? extends Number>>();
		for (Class <? extends Number> component : components) {
			al.add(component);
		}
		return Collections.unmodifiableList(al);
	}
	
	public static IBinaryEvent [] merge (IBinaryEvent[] ... arrays) {
		int size = 0;
		for (int i = 0; i < arrays.length; i++) {
			size += arrays[i].length;
		}
		
		IBinaryEvent [] all = new IBinaryEvent[size];
		
		int k = 0;
		for (int i = 0; i < arrays.length; i++) {
			for (int j = 0; j < arrays[i].length; j++) {
				all[k++] = arrays[i][j];
			}
		}
		
		return all;
	}
}

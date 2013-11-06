package it.unitn.disi.cli;

import it.unitn.disi.utils.logging.CodecUtils;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.EventCodec.DecodingStream;
import it.unitn.disi.utils.logging.RecordTypeSet;
import it.unitn.disi.utils.logging.IBinaryRecordType;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class TestEventDecoder {
	
	@Test
	@SuppressWarnings("unchecked")
	public void testEventDecoding() {
		Number [] eventStream = 
			{
				new Byte((byte)1), new Long(5), new Integer(6),								// GARBAGE
				new Byte((byte)0), new Long(1), new Long(2), new Integer(3), new Long(4),
				new Byte((byte)0), new Long(7), new Long(8), new Integer(9), new Long(10),	// GARBAGE
				new Byte((byte)1), new Long(5), new Integer(6),
				new Byte((byte)0), new Long(7), new Long(8), new Integer(9), new Long(10),	// GARBAGE
				new Byte((byte)0), new Long(7), new Long(8), new Integer(9), new Long(10),
				new Byte((byte)0), new Long(7), new Long(8), new Integer(9), new Long(10),	// GARBAGE
				new Byte((byte)2), new Long(11), new Long(12), new Long(13), new Long(14),
				new Byte((byte)0), new Long(7), new Long(8), new Integer(9), new Long(10) 	// GARBAGE
			};
		
		byte [] buf = new byte[9 + 24*(Long.SIZE/Byte.SIZE) + 8*(Integer.SIZE/Byte.SIZE)];
		
		int len = 0;
		for (Number number : eventStream) {
			len = append(number, buf, len);
		}
		
		EventCodec decoder = new EventCodec(Byte.class, new IBinaryRecordType[] {
			new TestEvent((byte)0, Long.class, Long.class, Integer.class, Long.class),
			new TestEvent((byte)1, Long.class, Integer.class),
			new TestEvent((byte)2, Long.class, Long.class, Long.class, Long.class)
		});
		
		ByteArrayInputStream is = new ByteArrayInputStream(buf);
		DecodingStream it = decoder.decodingStream(is);
		
		int i = 1;
		int j = 0;
		while (it.hasNext()) {
			Number num = it.next();
			if (num instanceof Byte) {
				if ((j++ % 2 == 0)) {
					it.skipEvent();
				}
				continue;
			}
			Assert.assertEquals(i++, num.intValue());
		}
		
		Assert.assertEquals(15, i);
	}
	
	private int append(Number number, byte[] buffer, int offset) {
		if (number instanceof Long) {
			return CodecUtils.append((Long) number, buffer, offset);
		} else if (number instanceof Integer) {
			return CodecUtils.append((Integer) number, buffer, offset);
		} else if (number instanceof Byte) {
			return CodecUtils.append((Byte) number, buffer, offset);
		}
		
		Assert.fail();
		
		return -1; // Shuts up the compiler.
	}
}

class TestEvent implements IBinaryRecordType {
	
	private ArrayList<Class<? extends Number>> fParts;
	
	private byte fMagic;
	
	public TestEvent(byte type, Class<? extends Number>...parts) {
		fMagic = type;
		fParts = new ArrayList<Class<? extends Number>>(parts.length);
		for (int i = 0; i < parts.length; i++) {
			fParts.add(parts[i]);
		}
	}

	public List<Class<? extends Number>> components() {
		return fParts;
	}

	public Byte magicNumber() {
		return fMagic;
	}

	@Override
	public RecordTypeSet<? extends Enum<? extends IBinaryRecordType>> eventSet() {
		return null;
	}

	@Override
	public String formattingString() {
		return null;
	}
	
}


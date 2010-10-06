package it.unitn.disi.utils.logging;

import it.unitn.disi.utils.EOFException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Helper class for type-checked, safe structured encoding and decoding of
 * binary events.
 * 
 * @author giuliano
 */
public class EventCodec {

	private static final Map<Class<? extends Number>, IPrimitiveCodec> fDecoderRegistry = new HashMap<Class<? extends Number>, IPrimitiveCodec>();

	private static final byte[] fBuffer;

	static {
		fDecoderRegistry.put(Byte.class, new ByteCodec());
		fDecoderRegistry.put(Integer.class, new FastIntegerCodec());
		fDecoderRegistry.put(Long.class, new FastLongCodec());

		int max = 0;
		for (IPrimitiveCodec decoder : fDecoderRegistry.values()) {
			max = Math.max(max, decoder.size());
		}
		fBuffer = new byte[max];
	}

	private final Map<Number, IBinaryEvent> fTypes = new HashMap<Number, IBinaryEvent>();

	private final IPrimitiveCodec fTypeDecoder;

	public EventCodec(Class<? extends Number> typeClass, IBinaryEvent[] types) {
		fTypeDecoder = lookupDecoder(typeClass);
		for (IBinaryEvent type : types) {
			registerType(type);
		}
	}

	public DecodingStream decodingStream(InputStream is) {
		return new DecodingStream(is);
	}

	public int encodeEvent(byte[] buffer, int startingOffset, Number... event) {
		IBinaryEvent type = typeCheck(event);

		// Encodes the type.
		int offset = unsafeEncode(fTypeDecoder.getType(), event[0], buffer,
				startingOffset);

		for (int i = 1; i < event.length; i++) {
			offset = unsafeEncode(type.components().get(i - 1), event[i],
					buffer, offset);
		}

		return offset;
	}

	public IBinaryEvent typeCheck(Number... event) {
		IBinaryEvent type = fTypes.get(event[0]);
		if (type == null) {
			throw new IllegalArgumentException("Unknown event type " + event[0]
					+ ".");
		}
		
		List<Class<? extends Number>> components = type.components();

		if (components.size() != (event.length - 1)) {
			throw new IllegalArgumentException("Expected " + (components.size() + 1)
					+ " parameters, got " + event.length + " instead.");
		}
		
		for (int i = 1; i < event.length; i++) {
			Class<? extends Number> partType = components.get(i - 1);
			Number value = event[i];
			// Type check.
			if (!(partType.isInstance(value))) {
				throw new IllegalArgumentException("Value " + value
						+ " does not match type " + type + ".");
			}
		}
		
		return type;
	}
	
	public String toString(Number...event) {
		IBinaryEvent type = typeCheck(event);
		String formatting = type.formattingString();
		if (formatting == null) {
			return "No formatting defined for event " + type.toString() + ".";
		}
		
		Object [] pars = new Object[event.length];
		pars[0] = type;
		System.arraycopy(event, 1, pars, 1, pars.length - 1);
		return String.format(type.formattingString(), pars);
	}

	private int unsafeEncode(Class<? extends Number> type, Number value,
			byte[] buffer, int offset) {
		IPrimitiveCodec codec = lookupDecoder(value.getClass());
		return codec.encode(value, buffer, offset);
	}

	private void registerType(IBinaryEvent type) {
		ArrayList<IPrimitiveCodec> list = new ArrayList<IPrimitiveCodec>();
		for (Class<? extends Number> klass : type.components()) {
			list.add(lookupDecoder(klass));
		}
		fTypes.put(type.magicNumber(), type);
	}

	private IPrimitiveCodec lookupDecoder(Class<? extends Number> klass) {
		if (!fDecoderRegistry.containsKey(klass)) {
			throw new NoSuchElementException("Don't know how to decode "
					+ klass.getName() + ".");
		}

		return fDecoderRegistry.get(klass);
	}

	public class DecodingStream implements Iterator<Number> {

		private InputStream fStream;

		private Number fCurrentType;

		private Number fNext;

		private IBinaryEvent fCurrentEventType;

		private int fDecodingIndex;

		private boolean fSeenEof;

		public DecodingStream(InputStream is) {
			fStream = is;
			fNext = readType();
		}

		public boolean skipEvent() {
			// We're not at the beginning of an event.
			//
			// Rationale for > 1: if the last call to next() returned the first 
			// element of an event, then either:
			//    - if the event had more than one component, then the second element
			//      has already been read, thus fDecodingIndex will be 1;
			//    - if the event has only one component, then the type of the next event
			//      will have already been read, and fDecodingIndex will be 0.
			//
			boolean wasBeginning = true;
			if (fDecodingIndex > 1) {
				wasBeginning = false;
			}

			// Skips till we reach the beginning of an event,
			// or we run out of stuff to read.
			//
			// Here we use 0 as stop condition because, when
			// the decoding index is zero, it means that the
			// next element returned by next() will be the 
			// type of the next event in the stream.
			while (hasNext() && fDecodingIndex != 0) {
				next();
			}

			return wasBeginning;
		}

		public boolean hasNext() {
			return !fSeenEof || fNext != null;
		}

		public Number next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			Number toReturn = fNext;
			fNext = readNext();

			return toReturn;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private Number readType() {
			fillBuffer(fBuffer, fTypeDecoder.size());
		
			if (fSeenEof) {
				return null;
			}
		
			fCurrentType = fTypeDecoder.decode(fBuffer);
			fCurrentEventType = fTypes.get(fCurrentType);
			if (fCurrentEventType == null) {
				throw new NoSuchElementException("Unknown event type ("
						+ fCurrentType + ").");
			}
		
			fDecodingIndex = 0;
			return fCurrentType;
		}

		private Number readNext() {
			List<Class<? extends Number>> components = fCurrentEventType.components();
			
			if (fDecodingIndex == components.size()) {
				return readType();
			}
		
			IPrimitiveCodec decoder = lookupDecoder(components.get(fDecodingIndex++));
			return decoder.decode(fillBuffer(fBuffer, decoder.size()));
		}

		private byte[] fillBuffer(byte[] buffer, int length) {
			int read = 0;

			while(true) {
				try {
					read += fStream.read(buffer, read, length - read);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			
				if (read == length) {
					break;
				} else if (read == -1) {
					fSeenEof = true;
					break;
				} else {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
			
			if (!fSeenEof && read < length) {
				throw new EOFException("Unexpected end-of-file while decoding.");
			}
		
			return buffer;
		}
	}
}

interface IPrimitiveCodec {

	public Class<? extends Number> getType();

	public int encode(Number number, byte[] buffer, int offset);

	public Number decode(byte[] encoded);

	public int size();

}

class ByteCodec implements IPrimitiveCodec {
	public Number decode(byte[] encoded) {
		return encoded[0];
	}

	public int encode(Number number, byte[] buffer, int offset) {
		return CodecUtils.append(number.byteValue(), buffer, offset);
	}

	public int size() {
		return 1;
	}

	public Class<? extends Number> getType() {
		return Byte.class;
	}
}

class FastLongCodec implements IPrimitiveCodec {
	public Number decode(byte[] encoded) {
		return CodecUtils.decodeLong(encoded, 0);
	}

	public int encode(Number number, byte[] buffer, int offset) {
		return CodecUtils.append(number.longValue(), buffer, offset);
	}

	public int size() {
		return Long.SIZE / Byte.SIZE;
	}

	public Class<? extends Number> getType() {
		return Long.class;
	}
}

class FastIntegerCodec implements IPrimitiveCodec {
	public Number decode(byte[] encoded) {
		return CodecUtils.decodeInt(encoded, 0);
	}

	public int encode(Number number, byte[] buffer, int offset) {
		return CodecUtils.append(number.intValue(), buffer, offset);
	}

	public int size() {
		return Integer.SIZE / Byte.SIZE;
	}

	public Class<? extends Number> getType() {
		return Integer.class;
	}
}

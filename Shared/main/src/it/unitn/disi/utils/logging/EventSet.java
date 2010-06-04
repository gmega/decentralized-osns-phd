package it.unitn.disi.utils.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * {@link EventSet} provides a number of useful functions for dealing with
 * logged {@link IBinaryEvent} events. It can answer questions such as what is
 * the size of an event, as well as perform reverse mapping by byte code.<BR>
 * <BR>
 * An {@link EventSet} always operates on an enumerated type, which we will call
 * the <i> associated enum </i>.
 * 
 * @author giuliano
 * 
 * @param <T>
 *            an {@link Enum} which implements {@link IBinaryEvent}, and
 *            contains the event collection to be operated upon.
 */
public class EventSet<T extends Enum<? extends IBinaryEvent>> {

	private final Map<Byte, EventEntry> fEvents;

	private final T fLargest;

	/**
	 * Creates a new event set for a given enumerated type which implements
	 * {@link IBinaryEvent}.
	 * 
	 * @param enumClass
	 *            the enumerated type to operate on top of.
	 */
	@SuppressWarnings("unchecked")
	public EventSet(Class<? extends Enum<? extends IBinaryEvent>> enumClass) {
		Object[] values = getValues(enumClass);

		fEvents = new HashMap<Byte, EventEntry>();

		int largest = Integer.MIN_VALUE;
		T largestEvt = null;
		for (int i = 0; i < values.length; i++) {
			IBinaryEvent evt = (IBinaryEvent) values[i];
			fEvents.put(evt.magicNumber(),
					new EventEntry(evt, computeSize(evt)));
			if (largest < sizeof(evt)) {
				largest = sizeof(evt);
				largestEvt = (T) evt;
			}
		}
		
		fLargest = largestEvt;
	}

	/**
	 * @param magic
	 *            an event byte type.
	 * 
	 * @return <b>true</b> if the byte type is known to this {@link EventSet}
	 *         (if {{@link #knows(byte)} returns <b>true</b> for that byte type.
	 */
	public boolean knows(byte magic) {
		return fEvents.containsKey(magic);
	}

	/**
	 * Answers the size (in bytes) for an {@link IBinaryEvent}.
	 * 
	 * @param type
	 *            the byte type of the {@link IBinaryEvent}, as returned by
	 *            {@link IBinaryEvent#magicNumber()}.
	 * 
	 * @throws IllegalArgumentException
	 *             if the event is not a part of the <i>associated enum</i>.
	 */
	public int sizeof(byte type) {
		EventEntry entry = checkedGet(type);
		return entry.size;
	}

	/**
	 * Convenience method. Same as <code>sizeof(evt.magicNumber())</code>.
	 */
	public int sizeof(IBinaryEvent evt) {
		return this.sizeof(evt.magicNumber());
	}

	/**
	 * @return the largest {@link IBinaryEvent} known to this event set.
	 */
	public T getLargest() {
		return fLargest;
	}

	/**
	 * @param type
	 *            a byte type.
	 * 
	 * @return the {@link IBinaryEvent} corresponding to that byte type.
	 * 
	 * @throws IllegalArgumentException
	 *             if the byte type is unknown to this {@link EventSet}.
	 */
	@SuppressWarnings("unchecked")
	public T event(byte type) {
		EventEntry entry = checkedGet(type);
		return (T) entry.event;
	}

	private int computeSize(IBinaryEvent evt) {
		int size = 0;
		for (Class<? extends Number> component : evt.components()) {
			try {
				Field field = component.getField("SIZE");
				size += ((Integer) field.get(component) / Byte.SIZE);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return size + 1;
	}

	private Object[] getValues(
			Class<? extends Enum<? extends IBinaryEvent>> enumClass) {
		try {
			Method values = enumClass.getMethod("values");
			Object[] vals = (Object[]) values.invoke(enumClass);
			return vals;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private EventEntry checkedGet(byte type) {
		EventEntry entry = fEvents.get(type);
		if (entry == null) {
			throw new IllegalArgumentException("Unknown event type " + type
					+ ".");
		}
		return entry;
	}

	private static class EventEntry {
		public final IBinaryEvent event;
		public final int size;

		public EventEntry(IBinaryEvent event, int size) {
			this.size = size;
			this.event = event;
		}
	}
}

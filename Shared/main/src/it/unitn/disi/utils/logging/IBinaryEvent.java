package it.unitn.disi.utils.logging;

import java.util.List;

/**
 * A binary event is an event which is composed by a collection of numbers. 
 * 
 * @author giuliano
 */
public interface IBinaryEvent {

	/**
	 * @return the value which must be the first to appear before any event.
	 *         It's used to differ the event from other types of events.
	 */
	public Byte magicNumber();

	/**
	 * @return a sequence which tells how to interpret the event.
	 */
	public List<Class<? extends Number>> components();

	/**
	 * Optional operation. If unsupported, will return <code>null</code>.
	 * <BR>
	 * @return a string such that String.format(formattingString(),
	 *         <components>) returns a complete string representation for an
	 *         event instance. 
	 */
	public String formattingString();

	/**
	 * @return the {@link EventSet} object associated to the parent {@link Enum}
	 *         class of this {@link IBinaryEvent}.
	 */
	public EventSet<? extends Enum<? extends IBinaryEvent>> eventSet();
}

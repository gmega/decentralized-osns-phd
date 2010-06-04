package it.unitn.disi;

import it.unitn.disi.application.NewscastEvents;
import it.unitn.disi.utils.logging.CodecUtils;
import it.unitn.disi.utils.logging.EventSet;
import it.unitn.disi.utils.logging.IBinaryEvent;

import java.util.List;

public enum SimulationEvents implements IBinaryEvent{
	/**
	 * Event type {@link NewscastEvents#NODE_JOIN} corresponds to a node
	 * (re-)joining the system.
	 * 
	 * <ol>
	 * <li>Long: id of the (re-)joining node.</li>
	 * <li>Long: instant in time for this event.</li>
	 * </ol>
	 */
	NODE_LOGIN((byte)0, Long.class, Long.class),

	/**
	 * Event type {@link NewscastEvents#NODE_DEPART} corresponds to a node
	 * departing from the system.
	 * 
	 * <ol>
	 * <li>Long: id of the departing node.</li>
	 * <li>Long: instant in time for this event.</li>
	 * </ol>
	 */
	NODE_DEPART((byte)1, Long.class, Long.class),

	/**
	 * Signals that a round has ended.
	 * 
	 * <ol>
	 * <li>Byte: event type.</li>
	 * <li>Long: number of the round that has just ended.</li>
	 * </ol>
	 */
	ROUND_ENDED((byte)2, Long.class);

	// ----------------------------------------------------------------------
	
	/** Associated event set providing extra functionality. */
	public static final EventSet<SimulationEvents> set = 
		new EventSet<SimulationEvents>(SimulationEvents.class);

	// ----------------------------------------------------------------------
	
	private final List<Class<? extends Number>> fComponents; 
	
	private final byte fType;
	
	private SimulationEvents(byte type, Class <? extends Number>...components) {
		fComponents = CodecUtils.mkROCollection(components);
		fType = type;
	}
	
	public List<Class<? extends Number>> components() {
		return fComponents;
	}
	
	public Byte magicNumber() {
		return fType;
	}

	public EventSet<? extends Enum<? extends IBinaryEvent>> eventSet() {
		return set;
	}

	public String formattingString() {
		return null;
	}
}

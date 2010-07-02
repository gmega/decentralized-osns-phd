package it.unitn.disi.application;

import it.unitn.disi.utils.logging.CodecUtils;
import it.unitn.disi.utils.logging.EventSet;
import it.unitn.disi.utils.logging.IBinaryEvent;

import java.util.List;

public enum NewscastEvents implements IBinaryEvent{
	/**
	 * Event type {@link NewscastEvents#TWEETED} corresponds to a new tweet
	 * being produced.
	 * 
	 * <ol>
	 * <li>Long: id of tweeting node.</li>
	 * <li>Int: sequence number of event in the tweeting node.</li>
	 * <li>Long: simulation time of the event.</li>
	 * </ol>
	 */
	TWEETED((byte)3, "%1$s: (%3$d) by %2$d @ %4$ds",
			Long.class, Integer.class, Long.class),
	
	/**
	 * Event type {@link NewscastEvents#DELIVER_SINGLE_TWEET} corresponds to a
	 * single tweet being delivered by the application layer.
	 * 
	 * <ol>
	 * <li>Long: id of tweeting node.</li>
	 * <li>Long: id of the sending node.</li>
	 * <li>Long: id of the receiving node.</li>
	 * <li>Int: sequence number of event in the tweeting node.</li>
	 * <li>Long: simulation time of the event.</li>
	 * </ol>
	 */
	DELIVER_SINGLE_TWEET((byte)4, "%1$s: (%2$d,%5$d) to %4$d from %3$d @ %6$d", 
			Long.class, Long.class, Long.class, Integer.class, Long.class),

	/**
	 * Event type {@link NewscastEvents#DELIVER_TWEET_RANGE} corresponds to a
	 * range of tweets being delivered by the application layer.
	 * <ol>
	 * <li>Byte: event type.</li>
	 * <li>Long: id of tweeting node.</li>
	 * <li>Long: id of the sending node.</li> 
	 * <li>Long: id of receiving node.</li>
	 * <li>Int: sequence number of the initial event delivered.</li>
	 * <li>Int: sequence number of the final event delivered.</li>
	 * <li>Long: simulation time of the event.</li>
	 * </ol>
	 */
	DELIVER_TWEET_RANGE((byte)5, null, Long.class, Long.class, Long.class, Integer.class, Integer.class, Long.class),
	
	/**
	 * Event {@link NewscastEvents#EXCHANGE_DIGESTS} corresponds to an exchange
	 * of list digests during an anti-entropy exchange.
	 * 
	 * <ol>
	 * <li>Byte: event type.</li>
	 * <li>Long: id of sending node.</li>
	 * <li>Long: id of receiving node.</li>
	 * <li>Int: Number of items in the digest.</li>
	 * <li>Long: simulation time of the event.</li>
	 * </ol>
 	 */
	EXCHANGE_DIGESTS((byte)6, null, Long.class, Long.class, Integer.class, Long.class),

	/**
	 * Event {@link NewscastEvents#SPURIOUS_TWEET} corresponds to a duplicate
	 * (already known) rumor that has been sent by one node to the other.
	 * 
	 * <ol>
	 * <li>Byte: event type.</li>
	 * <li>Long: id of sending node.</li>
	 * <li>Long: id of receiving node.</li>
	 * <li>Long: id of the tweet producer.</li>
	 * <li>Int: Sequence number of the duplicate tweet.</li>
	 * <li>Long: simulation time of the event.</li>
	 * </ol>
	 */
	DUPLICATE_TWEET((byte)7, "%1$s: (%4$d,%5$d) to %3$d from %2$d @ %6$d", 
			Long.class, Long.class, Long.class, Integer.class, Long.class);
	
	// ----------------------------------------------------------------------
	
	/** Associated event set providing extra functionality. */
	public static final EventSet<NewscastEvents> set = 
		new EventSet<NewscastEvents>(NewscastEvents.class);

	// ----------------------------------------------------------------------
	
	private final List<Class<? extends Number>> fComponents; 
	
	private final byte fType;
	
	private final String fFormat;
	
	private NewscastEvents(byte type, String format, Class <? extends Number>...components) {
		fType = type;
		fFormat = format;
		fComponents = CodecUtils.mkROCollection(components);
	}
	
	public List<Class<? extends Number>> components() {
		return fComponents;
	}
	
	public Byte magicNumber() {
		return fType;
	}
	
	public String formattingString() {
		return fFormat;
	}
	
	public EventSet<? extends Enum<? extends IBinaryEvent>> eventSet() {
		return set;
	}
}

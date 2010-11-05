package peersim.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a non-static attribute or a constructor parameter into an entry in the
 * PeerSim configuration. <BR>
 * <BR>
 * As an example, suppose we had a class <code>Example</code>: <BR>
 * <code>
 * 	@AutoConfig
 * 	class Example {
 * 		@Attribute("some_string")
 * 		private String s;
 * 
 * 		@Attribute(Attribute.AUTO)
 * 		private int linkable;
 *  }
 *  </code>
 *  <BR>
 *  and the following configuration file:
 *  <BR>
 *  <code>
 *  protocol.other SomeClass
 *  
 *  protocol.example Example
 *  example.some_string some value
 *  example.linkable other
 *  </code>
 *  <BR>
 *  This would cause the configurator to assign "some value" to <code>Example.s</code>, and
 *  the protocol id of <code>other</code> to <code>Example.linkable</code>.<BR>
 *  <BR>
 *  A similar effect could be achieved by:
 *  <code>
 *  @AutoConfig
 *  class Example {
 *  	public Example(@Attribute("some_string") String a_string,
 *  				@Attribute("linkable") int linkable) {
 *  		...
 *  	}
 *  }
 *  </code>
 *  
 *  This would cause the configurator to call the constructor of <code>Example</code> 
 *  and "some value" and the protocol id of <code>other</code> to <code>a_string</code>
 *  and <code>linkable</code>, respectively.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
public @interface Attribute {

	// -------------------------------------------------------------------------
	
	/**
	 * Causes the configurator to map the name of the annotated attribute to an
	 * entry with the same name in the configuration file.
	 */
	public static final String AUTO = "__AUTO__";

	/**
	 * Causes the property to be filled in by the configurator with the
	 * configuration prefix for the caller class (which can then be used in
	 * calls to {@link Configuration}).
	 */
	public static final String PREFIX = "__PREFIX__";

	/**
	 * Name for the attribute in the configuration context. Defaults to
	 * {@link #AUTO}. Might also be set to the special value {@link #PREFIX}.
	 */
	String value() default AUTO;
	
	// --------------------------------------------------------------------------
	/**
	 * Special value used internally to tell the configurator that the attribute
	 * has no default value.
	 */
	public static final String VALUE_NONE = "__NONE__";
	
	/**
	 * Default value for a configuration attribute.
	 */
	String defaultValue() default VALUE_NONE;
	
	// --------------------------------------------------------------------------
}

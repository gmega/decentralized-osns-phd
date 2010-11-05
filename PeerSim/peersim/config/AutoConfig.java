package peersim.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which indicates to the PeerSim configurator that it should try to
 * automatically match attributes in the configuration to fields and
 * constructors in this class. <BR>
 * <BR>
 * Matching is directed by the use of annotation {@link Attribute}.
 * 
 * @see Attribute
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoConfig {
}

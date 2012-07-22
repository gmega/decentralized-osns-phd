package it.unitn.disi.simulator.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An {@link IEventObserver} annotated as {@link Binding} is treated specially
 * by the simulation engine in that the simulation loop will not stop until it
 * calls {@link EDSimulationEngine#unbound(IEventObserver)}.
 * 
 * @author giuliano
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Binding {

}

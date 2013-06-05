package it.unitn.disi.simulator.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An {@link IEventObserver} annotated as {@link Binding} is treated specially
 * by the simulation engine in that the simulation loop will not stop until it
 * calls {@link EDSimulationEngine#unbound(IEventObserver)}.
 * 
 * @author giuliano
 * 
 * @deprecated this is a complicated, inflexible, and unnecessary mechanism. Use
 *             {@link EngineBuilder#addObserver(IEventObserver, int, boolean, boolean)}
 *             instead, setting the 'binding' parameter to true.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Binding {

}

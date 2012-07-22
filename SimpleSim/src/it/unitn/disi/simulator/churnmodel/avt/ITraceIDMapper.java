package it.unitn.disi.simulator.churnmodel.avt;

/**
 * {@link ITraceIDMapper} knows how to assign process IDs to AVT trace node IDs.
 * 
 * @author giuliano
 * 
 */
public interface ITraceIDMapper {
	public String idOf(int pid);
}

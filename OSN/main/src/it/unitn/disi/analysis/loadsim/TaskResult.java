package it.unitn.disi.analysis.loadsim;

import java.util.Collection;

/**
 * Tuple representing the results for a run of {@link ExperimentRunner}.
 * 
 * @author giuliano
 */
public class TaskResult {
	public final int duration;
	public final UnitExperiment root;
	public final Collection<? extends MessageStatistics> statistics;

	public TaskResult(int duration, UnitExperiment root,
			Collection<? extends MessageStatistics> statistics) {
		this.duration = duration;
		this.root = root;
		this.statistics = statistics;
	}
}

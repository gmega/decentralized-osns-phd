package it.unitn.disi.analysis.loadsim;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * {@link UniformRateScheduler} ensures that a certain percentage of the
 * neighborhood of a node will generate traffic for the duration of the main
 * experiment.
 * 
 * @author giuliano
 */
@AutoConfig
public class UniformRateScheduler implements IScheduler {

	private final ILoadSim fParent;

	private final Random fRandom;

	private final double fTweetProbability;
	
	private final UnitExperiment fRoot;
	
	private volatile int fRepeats;

	public UniformRateScheduler(@Attribute("parent") ILoadSim parent, 
			@Attribute("root") UnitExperiment root,
			@Attribute("random") Random random, 
			@Attribute("probability") double probability,
			@Attribute("repetitions") int repeats) {
		fParent = parent;
		fTweetProbability = probability;
		fRandom = random;
		fRoot = root;
		fRepeats = repeats;
	}

	@Override
	public List<UnitExperiment> atTime(int round) {
		if (isOver()) {
			throw new NoSuchElementException();
		}

		List<UnitExperiment> experiments = new ArrayList<UnitExperiment>();
		for (Integer participant : fRoot.participants()) {
			if (shouldSchedule()) {
				experiments.add(fParent.unitExperiment(participant));
			}
		}

		return experiments;
	}

	private boolean shouldSchedule() {
		return fRandom.nextDouble() < fTweetProbability;
	}

	@Override
	public boolean isOver() {
		return fRepeats == 0;
	}

	@Override
	public boolean experimentDone(UnitExperiment experiment) {
		if (experiment == fRoot) {
			fRepeats--;
		}

		return isOver();
	}

}

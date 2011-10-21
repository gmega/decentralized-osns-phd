package it.unitn.disi.unitsim.experiments;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.plugin.IPlugin;
import it.unitn.disi.network.churn.yao.ISeedStream;
import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.ed.EDGovernor;
import it.unitn.disi.unitsim.ed.IEDUnitExperiment;

@AutoConfig
public class EDSeedStream implements IPlugin, ISeedStream,
		IExperimentObserver<IEDUnitExperiment> {

	private int fRepeats;

	private int fNextSeed;
	
	private long fCurrentSeed;
	
	private boolean fShouldReseed;

	private Random fRandom;
	
	private EDGovernor fGovernor;

	public EDSeedStream(@Attribute("repetitions") int reps,
			@Attribute("Random") Random random,
			@Attribute("EDGovernor") EDGovernor governor) {
		fRepeats = reps;
		fRandom = random;
		fGovernor = governor;
	}

	@Override
	public void experimentStart(IEDUnitExperiment experiment) {
		fShouldReseed = true;
	}

	@Override
	public void experimentEnd(IEDUnitExperiment experiment) {
		fNextSeed--;
	}

	@Override
	public long nextSeed() {
		if (fNextSeed == 0) {
			fCurrentSeed = fRandom.nextLong();
			fNextSeed = fRepeats;
		}
		fShouldReseed = false;
		return fCurrentSeed;
	}

	@Override
	public boolean shouldReseed() {
		return fShouldReseed;
	}

	@Override
	public String id() {
		return "SeedStream";
	}

	@Override
	public void start(IResolver resolver) throws Exception {
		fGovernor.addExperimentObserver(this);
	}

	@Override
	public void stop() throws Exception {
	}

}

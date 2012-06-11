package it.unitn.disi.unitsim.experiments;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.plugin.IPlugin;
import it.unitn.disi.simulator.yao.ISeedStream;
import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.ed.EDGovernor;
import it.unitn.disi.unitsim.ed.IEDUnitExperiment;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.tabular.ITableWriter;

@AutoConfig
@StructuredLog(key = "SD", fields = { "seed_no", "value" })
public class EDSeedStream implements IPlugin, ISeedStream,
		IExperimentObserver<IEDUnitExperiment> {

	private int fRepeats;

	private int fNextSeed;
	
	private int fReseeds;

	private long fCurrentSeed;

	private boolean fShouldReseed = true;

	private Random fRandom;

	private EDGovernor fGovernor;
	
	private ITableWriter fWriter;

	public EDSeedStream(@Attribute("repetitions") int reps,
			@Attribute("Random") Random random,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute("EDGovernor") EDGovernor governor) {
		fRepeats = reps;
		fRandom = random;
		fGovernor = governor;
		fWriter = manager.get(EDSeedStream.class);
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
			fWriter.set("seed_no", fReseeds);
			fWriter.set("value", fCurrentSeed);
			fWriter.emmitRow();
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

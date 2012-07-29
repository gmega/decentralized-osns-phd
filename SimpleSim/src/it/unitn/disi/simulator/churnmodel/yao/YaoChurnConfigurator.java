package it.unitn.disi.simulator.churnmodel.yao;

import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IDistributionGenerator;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.utils.collections.Triplet;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;

@AutoConfig
public class YaoChurnConfigurator {

	@Attribute(value = "yaomode")
	protected String fMode;

	@Attribute(value = "assignments", defaultValue = "yao")
	protected String fAssignments;

	public YaoChurnConfigurator() {
	}

	public YaoChurnConfigurator(String mode, String assignments) {
		fMode = mode;
		fAssignments = assignments;
	}

	public synchronized IAverageGenerator averageGenerator() {
		if (fAssignments.toLowerCase().equals("yao")) {
			return YaoPresets.averageGenerator("yao", CommonState.r);
		}

		throw new IllegalArgumentException(fAssignments);
	}

	public synchronized IDistributionGenerator distributionGenerator() {
		return YaoPresets.mode(fMode.toUpperCase(), new Random());
	}

	public synchronized IDistributionGenerator distributionGenerator(
			Random random) {
		return YaoPresets.mode(fMode.toUpperCase(), random);
	}

	public Triplet<double[], double[], IProcess[]> createProcesses(int n) {
		return createProcesses(n, new Random());
	}

	public Triplet<double[], double[], IProcess[]> createProcesses(int n,
			Random random) {
		double[] li = new double[n];
		double[] di = new double[n];

		IAverageGenerator generator = averageGenerator();

		for (int i = 0; i < n; i++) {
			li[i] = generator.nextLI();
			di[i] = generator.nextDI();
		}

		return new Triplet<double[], double[], IProcess[]>(li, di,
				createProcesses(li, di, n, random));
	}

	public IProcess[] createProcesses(double[] li, double[] di, int n) {
		return createProcesses(li, di, n, new Random());
	}

	public IProcess[] createProcesses(double[] li, double[] di, int n,
			Random random) {

		IProcess[] rp = new IProcess[n];
		IDistributionGenerator generator = distributionGenerator(random);

		for (int i = 0; i < rp.length; i++) {
			rp[i] = new RenewalProcess(i, uptimeDistribution(generator, li[i]),
					downtimeDistribution(generator, di[i]), State.down);
		}

		return rp;
	}

	private IDistribution downtimeDistribution(IDistributionGenerator distGen,
			double d) {
		return distGen.downtimeDistribution(d);
	}

	private IDistribution uptimeDistribution(IDistributionGenerator distGen,
			double d) {
		return distGen.uptimeDistribution(d);
	}
}

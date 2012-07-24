package it.unitn.disi.network.churn.yao;

import java.util.HashMap;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.config.resolvers.CompositeResolver;

import it.unitn.disi.simulator.churnmodel.yao.ISeedStream;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.utils.HashMapResolver;
import it.unitn.disi.utils.MiscUtils;

@AutoConfig
public class ScriptableGenerator implements IAverageGenerator, IDistribution {

	private ISeedStream fSeedStream;

	private IDistribution fDistribution;

	private Random fRandom = new Random();

	public ScriptableGenerator(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute IResolver resolver,
			@Attribute("distribution") String generatorClass,
			@Attribute("SeedStream") ISeedStream seedStream) {

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("UniformDistribution", this);

		CompositeResolver composite = new CompositeResolver();
		composite.addResolver(new HashMapResolver(map), resolver);

		IResolver scoped = composite.asResolver();
		
		fSeedStream = seedStream;
		fDistribution = (IDistribution) ObjectCreator.createInstance(
				getClass(generatorClass), prefix + ".distribution", scoped);
	}

	@Override
	public double nextLI() {
		reseedIfNeeded();
		return fDistribution.sample();
	}

	@Override
	public double nextDI() {
		reseedIfNeeded();
		return fDistribution.sample();
	}

	@Override
	public String id() {
		return ScriptableGenerator.class.getSimpleName();
	}

	@Override
	public double sample() {
		return fRandom.nextDouble();
	}

	@Override
	public double expectation() {
		return 0.5;
	}

	private Class<?> getClass(String generatorClass) {
		try {
			return Class.forName(generatorClass);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	private void reseedIfNeeded() {
		if (fSeedStream.shouldReseed()) {
			fRandom.setSeed(fSeedStream.nextSeed());
		}
	}
}

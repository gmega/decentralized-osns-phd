package it.unitn.disi.churn.config;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.utils.MiscUtils;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

@AutoConfig
public class GenerateAssignments implements Runnable {

	private YaoChurnConfigurator fYaoChurn;

	private int fN;

	private String fOutput;

	public GenerateAssignments(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("n") int n, @Attribute("output") String output) {
		fYaoChurn = ObjectCreator.createInstance(YaoChurnConfigurator.class,
				"", resolver);
		fOutput = output;
		fN = n;
	}

	@Override
	public void run() {
		IAverageGenerator generator = fYaoChurn.averageGenerator(new Random());
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(new File(fOutput), "rw");
			generate(file, generator);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			MiscUtils.safeClose(file, false);
		}
	}

	private void generate(RandomAccessFile stream, IAverageGenerator generator)
			throws IOException {
		for (int i = 0; i < fN; i++) {
			stream.writeDouble(generator.nextLI());
			stream.writeDouble(generator.nextDI());
		}
	}
}

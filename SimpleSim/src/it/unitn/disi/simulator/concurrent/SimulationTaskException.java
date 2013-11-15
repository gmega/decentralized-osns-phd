package it.unitn.disi.simulator.concurrent;

import java.io.PrintStream;
import java.util.Map;

import it.unitn.disi.simulator.core.ISimulationEngine;

public class SimulationTaskException extends Exception {

	private static final long serialVersionUID = 1L;

	private final Map<String, ? extends Object> fProperties;

	private final ISimulationEngine fEngine;

	public SimulationTaskException(Map<String, ? extends Object> properties,
			ISimulationEngine engine, Exception cause) {
		super(cause);
		fProperties = properties;
		fEngine = engine;
	}

	public ISimulationEngine getEngine() {
		return fEngine;
	}

	public Map<String, ? extends Object> properties() {
		return fProperties;
	}
	
	public void dumpProperties(PrintStream stream) {
		stream.println("# Simulation property dump");
		for (String key : fProperties.keySet()) {
			stream.print(key);
			stream.print(" ");
			stream.println(fProperties.get(key).toString());
		}
	}
}

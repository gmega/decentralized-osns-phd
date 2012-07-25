package it.unitn.disi.simulator.churnmodel.avt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import it.unitn.disi.simulator.churnmodel.IProcessFactory;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class AVTChurnConfigurator implements IProcessFactory {

	private String fAssignmentMode;

	private String fTracefile;

	private double fScale;

	private long fCut;

	private boolean fLoop;

	private Pair<Map<String, long[]>, Long> fTraces;

	public AVTChurnConfigurator(
			@Attribute("assignment_mode") String assignmentMode,
			@Attribute("tracefile") String tracefile,
			@Attribute("assignment_mode") String assignment,
			@Attribute(value = "timescale", defaultValue = "1.0") double timescale,
			@Attribute(value = "time_cut", defaultValue = "9223372036854775807") long cut,
			@Attribute(value = "loop") boolean loop) {
		fTracefile = tracefile;
		fScale = timescale;
		fCut = cut;
		fLoop = loop;
		fAssignmentMode = assignmentMode;
	}

	@Override
	public IProcess[] createProcesses(int n) {
		ITraceIDMapper mapper = getAssignment(n);
		IProcess[] processes = new IProcess[n];
		for (int i = 0; i < n; i++) {
			long[] trace = traces().a.get(mapper.idOf(n));
			ArrayIterator it = new ArrayIterator(trace);
			if (fLoop) {
				it.setSync(traces().b);
			}
			DistributionPair pair = new DistributionPair(it);
			processes[i] = new RenewalProcess(i, pair.up, pair.down, State.down);
		}
		return processes;
	}

	private Pair<Map<String, long[]>, Long> traces() {
		try {
			if (fTraces == null) {
				fTraces = AVTDecoder.decode(new FileInputStream(new File(
						fTracefile)), fCut);
			}
		} catch (IOException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
		return fTraces;
	}

	private ITraceIDMapper getAssignment(final int n) {
		Map<String, long[]> traces = traces().a;
		final ArrayList<String> traceIds = new ArrayList<String>();
		
		// Shuffles to get rid of hash bias.
		traceIds.addAll(traces.keySet());
		Collections.shuffle(traceIds);
		return new ITraceIDMapper() {
			@Override
			public String idOf(int pid) {
				return traceIds.get(pid % (traceIds.size()));
			}
		};
	}

	public static class DistributionPair {

		private enum LastSampled {
			up, down;
		}

		private LastSampled fState = LastSampled.up;

		private final ArrayIterator fIterator;

		private long fBaseTime;

		private DistributionPair(ArrayIterator iterator) {
			fIterator = iterator;
		}

		public final IDistribution up = new IDistribution() {

			@Override
			public double sample() {
				if (fState == LastSampled.up) {
					throw new IllegalStateException();
				}

				fState = LastSampled.up;

				return nextEvent();
			}

			@Override
			public double expectation() {
				return Double.NaN;
			}
		};

		public final IDistribution down = new IDistribution() {

			@Override
			public double sample() {
				if (fState == LastSampled.down) {
					throw new IllegalStateException();
				}

				fState = LastSampled.down;

				return nextEvent();
			}

			@Override
			public double expectation() {
				return Double.NaN;
			}
		};

		private long nextEvent() {
			long next = fIterator.next();
			long sample = next - fBaseTime;
			fBaseTime = next;
			return sample;
		}

	}

}

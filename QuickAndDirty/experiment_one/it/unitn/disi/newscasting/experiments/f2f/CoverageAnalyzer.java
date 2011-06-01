package it.unitn.disi.newscasting.experiments.f2f;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.newscasting.experiments.IExperimentObserver;
import it.unitn.disi.utils.SparseMultiCounter;
import it.unitn.disi.utils.logging.StructuredLog;

@AutoConfig
@StructuredLog(key = "COV", fields = { "id", "degree", "direct", "indirect",
		"unseen", "mincover", "maxcover", "avgcover" })
public class CoverageAnalyzer implements IExperimentObserver {

	private int fOneHop;

	public CoverageAnalyzer() {
	}

	@Override
	public void experimentStart(Node root) {
	}

	@Override
	public void experimentCycled(Node root) {
	}

	@Override
	public void experimentEnd(Node root) {
		SparseMultiCounter<Node> twoHopSeen = new SparseMultiCounter<Node>();
		Set<Node> oneHopSeen = new HashSet<Node>();

		Linkable onehop = (Linkable) root.getProtocol(fOneHop);
		int degree = onehop.degree();
		for (int i = 0; i < degree; i++) {
			Node friend = onehop.getNeighbor(i);

			// Was friend reached?
			if (!isResidue(friend)) {
				oneHopSeen.add(friend);
				continue;
			}

			// Friend wasn't reached, but maybe a F2F
			// was reached.
			Linkable twohop = (Linkable) friend.getProtocol(fOneHop);
			int tDegree = twohop.degree();
			for (int j = 0; j < tDegree; j++) {
				Node fof = twohop.getNeighbor(j);
				if (!isResidue(fof)) {
					twoHopSeen.increment(friend);
				}
			}
		}
	}

	private boolean isResidue(Node fof) {
		// TODO Auto-generated method stub
		return false;
	}

}

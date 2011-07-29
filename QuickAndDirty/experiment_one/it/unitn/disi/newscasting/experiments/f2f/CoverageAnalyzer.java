package it.unitn.disi.newscasting.experiments.f2f;

import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.unitsim.ICDExperimentObserver;
import it.unitn.disi.unitsim.ICDUnitExperiment;
import it.unitn.disi.unitsim.experiments.NeighborhoodExperiment;
import it.unitn.disi.utils.SparseMultiCounter;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;

@AutoConfig
@StructuredLog(key = "COV", fields = { "id", "degree", "direct", "indirect",
		"unseen", "mincover", "maxcover", "avgcover" })
public class CoverageAnalyzer implements ICDExperimentObserver {

	@Attribute("application")
	private int fApplication;

	@Attribute("onehop")
	private int fOneHop;

	private final TableWriter fLog;

	public CoverageAnalyzer(
			@Attribute("TabularLogManager") TabularLogManager mgr) {
		fLog = mgr.get(CoverageAnalyzer.class);
	}

	@Override
	public void experimentStart(ICDUnitExperiment exp) {
	}

	@Override
	public void experimentCycled(ICDUnitExperiment exp) {
	}

	@Override
	public void experimentEnd(ICDUnitExperiment exp) {
		NeighborhoodExperiment nexp = (NeighborhoodExperiment) exp;
		Node root = nexp.rootNode();
		
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

		Set<Node> indirect = twoHopSeen.asMap().keySet();

		fLog.set("id", root.getID());
		fLog.set("degree", onehop.degree());
		fLog.set("direct", oneHopSeen.size());
		fLog.set("indirect", indirect.size());

		oneHopSeen.addAll(indirect);
		fLog.set("unseen", onehop.degree() - oneHopSeen.size());

		int sumCover = 0;
		int minCover = Integer.MAX_VALUE;
		int maxCover = Integer.MIN_VALUE;
		Iterator<Node> it = indirect.iterator();
		while (it.hasNext()) {
			int cover = twoHopSeen.count(it.next());
			sumCover += cover;
			minCover = Math.min(minCover, cover);
			maxCover = Math.max(maxCover, cover);
		}

		fLog.set("mincover", minCover);
		fLog.set("maxcover", maxCover);
		fLog.set("avgcover", sumCover / ((double) indirect.size()));
		fLog.emmitRow();
	}

	private boolean isResidue(Node fof) {
		IApplicationInterface pset = (IApplicationInterface) fof
				.getProtocol(fApplication);
		return pset.storage().elements() == 0;
	}

}

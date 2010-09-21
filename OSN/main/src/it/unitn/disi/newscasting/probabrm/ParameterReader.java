package it.unitn.disi.newscasting.probabrm;

import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ParameterReader implements NodeInitializer, Control {
	
	private static final String PAR_FILE = "file";
	
	private static final String PAR_ADAPTABLE = "adaptable";
	
	private Multimap<Long, Pair<Long, Double>> fFwProbabilities = HashMultimap.create(); 
	
	private Multimap<Long, Pair<Long, Double>> fSnProbabilities = HashMultimap.create();
	
	private int fAdaptableId;
	
	private String fFileName;

	public ParameterReader(String prefix) {
		fAdaptableId = Configuration.getPid(prefix + "." + PAR_ADAPTABLE);
		fFileName = Configuration.getString(prefix + "." + PAR_FILE);
	}
	
	public boolean execute() {
		InputStream stream = null;
		try {
			stream = new FileInputStream(new File(fFileName));
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream));
			String line;
			int lineCount = 1;
			while ((line = reader.readLine()) != null) {
				String [] triplet = line.split(" ");
				if (triplet.length != 3) {
					System.err.println("Malformed line " + lineCount + " has been ignored.");
				}
				
				Long sender = Long.parseLong(triplet[0]);
				Long receiver = Long.parseLong(triplet[1]);
				Double probability = Double.parseDouble(triplet[2]);
				fSnProbabilities.put(sender, new Pair<Long, Double>(receiver, probability));
				fFwProbabilities.put(receiver, new Pair<Long, Double>(sender, probability));
				lineCount++;
			}
		} catch(Exception ex) { 
			throw new RuntimeException(ex);
		} finally {
			MiscUtils.safeClose(stream, true);
		}
		
		for (int i = 0; i < Network.size(); i++) {
			initialize(Network.get(i));
		}
		
		return false;
	}

	public void initialize(Node node) {
		ICoreInterface adaptable = (ICoreInterface) node.getProtocol(fAdaptableId);
		ProbabilisticRumorMonger prm = (ProbabilisticRumorMonger) adaptable
				.getStrategy(ProbabilisticRumorMonger.class);
		
		prm.setNodeId(node.getID());
		
		for (Pair<Long, Double> pair : fSnProbabilities.get(node.getID())) {
			prm.setSendingProbability(pair.a, pair.b);
		}
		
		for (Pair<Long, Double> pair : fFwProbabilities.get(node.getID())) {
			prm.setForwardProbability(pair.a, pair.b);
		}
	}
}


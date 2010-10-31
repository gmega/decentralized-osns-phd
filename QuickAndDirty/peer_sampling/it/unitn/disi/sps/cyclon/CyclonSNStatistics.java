package it.unitn.disi.sps.cyclon;

import it.unitn.disi.cli.ITransformer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

@AutoConfig
public class CyclonSNStatistics implements ITransformer{
	
	private Map<String, NodeStats> statistics = new HashMap<String, NodeStats>();

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		String line = null;
		while ((line = reader.readLine()) != null) {
			String [] parts = line.split(" ");
			String id = parts[0];
			String degree = parts[1];
			int indegree = Integer.parseInt(parts[2]);
			
			getStats(id, degree).indegree.add(indegree);
		}
		
		System.out.println("id degree min max avg var");
		
		for (NodeStats stat : statistics.values()) {
			StringBuffer sbuffer = new StringBuffer();
			
			sbuffer.append(stat.id);
			sbuffer.append(" ");
			sbuffer.append(stat.degree);
			sbuffer.append(" ");
			sbuffer.append(stat.indegree.getMin());
			sbuffer.append(" ");
			sbuffer.append(stat.indegree.getMax());
			sbuffer.append(" ");
			sbuffer.append(stat.indegree.getAverage());
			sbuffer.append(" ");
			sbuffer.append(stat.indegree.getVar());
			
			System.out.println(sbuffer);
		}
	}
	
	private NodeStats getStats(String id, String degree) { 
		NodeStats stat = statistics.get(id);
		if (stat == null) {
			stat = new NodeStats(id, degree);
			statistics.put(id, stat);
		}
		
		return stat;
	}
}

class NodeStats {
	public final String id;
	public final String degree;
	public final IncrementalStats indegree;
	
	public NodeStats(String id, String degree) {
		super();
		this.id = id;
		this.degree = degree;
		this.indegree = new IncrementalStats();
	}
}
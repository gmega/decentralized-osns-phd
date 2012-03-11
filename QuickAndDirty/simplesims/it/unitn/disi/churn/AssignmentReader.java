package it.unitn.disi.churn;

import java.io.IOException;
import java.io.InputStream;

public class AssignmentReader extends SequentialAttributeReader<double[][]>{

	public static final int LI = 0;
	public static final int DI = 1;
	
	public AssignmentReader(InputStream stream, String id) throws IOException {
		super(stream, id);
	}

	@Override
	public double[][] read(int[] ids) throws IOException {
		
		double [][] lidi = new double[ids.length][2];
		for (int i = 0; !rootChanged(); i++) {		
			int node = idOf(Integer.parseInt(get("node")), ids);
			lidi[node][LI] = Double.parseDouble(get("li"));
			lidi[node][DI] = Double.parseDouble(get("di"));
			advance();
		}
		
		return lidi;
	}

}

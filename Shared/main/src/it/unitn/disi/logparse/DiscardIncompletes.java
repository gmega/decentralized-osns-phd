package it.unitn.disi.logparse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;

import it.unitn.disi.cli.ITransformer;

public class DiscardIncompletes implements ITransformer {
		
	private final ITag fEndMarker;
	
	private final LinkedList<Object> fOpenExperiments = new LinkedList<Object>();
	
	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		
		long printTo = establishLastOffset(is);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		String line;
		
		while ((line = reader.readLine()))
	}
	
	class PendingExperiment {
		Object id;
		int startPosition;
		int endPosition = -1;
		
		public PendingExperiment(Object id, int startPosition) {
			this.id = id;
			this.startPosition = startPosition;
		}
	}
}

interface ITag {
	public Object idFrom(String line);
}

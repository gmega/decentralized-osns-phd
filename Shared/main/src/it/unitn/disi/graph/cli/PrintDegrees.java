package it.unitn.disi.graph.cli;

import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Prints node degrees.
 * 
 * @author giuliano
 */
@AutoConfig
public class PrintDegrees extends GraphAnalyzer {
	
	@Attribute(value = "no_header", defaultValue = "false")
	boolean no_header;
	
	public PrintDegrees(@Attribute("decoder") String decoder) {
		super(decoder);
	}

	@Override
	protected void transform(LightweightStaticGraph graph, OutputStream oup)
			throws IOException {
		PrintStream out = new PrintStream(oup);		
		if(!no_header) {
			out.println("id degree");
		}
		
		try {
			for (int i = 0; i < graph.size(); i++) {
				out.println(i + " " + graph.degree(i));
			}
		} finally {
			oup.close();
		}
	}
}

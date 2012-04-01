package it.unitn.disi.churn.connectivity.p2p;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import java.util.Collections;

import peersim.config.AutoConfig;

import it.unitn.disi.cli.ITransformer;

@AutoConfig
public class P2PShuffleSources implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		ArrayList<String> lines = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		// First line is treated differently.
		String header = reader.readLine();
		
		String line;
		while ((line = reader.readLine()) != null) {
			lines.add(line);
		}

		PrintStream out = new PrintStream(oup);
		Collections.shuffle(lines);
		out.println(header);
		for (int i = 0; i < lines.size(); i++) {
			out.println(lines.get(i));
		}
	}

}

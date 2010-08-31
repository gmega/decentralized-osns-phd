package it.unitn.disi.newscasting;

import it.unitn.disi.application.SimpleApplication;
import it.unitn.disi.utils.peersim.PeersimUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class DeterministicSelectionReader implements Control {
	
	private static final String PAR_FILE = "file";
	
	private static final String PAR_DETERMINISTIC_SEL = "deterministic_selector";
	
	private static final String PAR_APP = "application";

	private File fFile;

	private int fSelectorId;
	
	private int fAppId;
	
	public DeterministicSelectionReader(String prefix) {
		fFile = new File(Configuration.getString(prefix + "." + PAR_FILE));
		fSelectorId = Configuration.getPid(prefix + "." + PAR_DETERMINISTIC_SEL);
		fAppId = Configuration.getPid(prefix + "." + PAR_APP);
	}

	public boolean execute() {
		try {
			return execute0();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private boolean execute0() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fFile));
		
		// Disables tweeting for everyone.
		for (int i = 0; i < Network.size(); i++) {
			((SimpleApplication) Network.get(i).getProtocol(fAppId)).suppressTweeting(true);
		}
		
		String line = reader.readLine();
		String spec [] = line.split(" ");

		// First line specifies who tweets.
		for (int i = 0; i < spec.length; i++) {
			long nodeId = Long.parseLong(spec[i]);
			SimpleApplication app = (SimpleApplication) PeersimUtils
				.lookupNode(nodeId).getProtocol(fAppId);
			app.suppressTweeting(false);			
		}	

		// Other lines specify the choices. 
		while ((line = reader.readLine()) != null) {
			spec = line.split(" ");
			long nodeId = Long.parseLong(spec[0]);
			DeterministicSelector selector = (DeterministicSelector) PeersimUtils
			.lookupNode(nodeId).getProtocol(fSelectorId);
			for (int i = 1; i < spec.length; i++) {
				selector.addChoice(spec[i]);
			}
		}
		
		return false;
	}
}

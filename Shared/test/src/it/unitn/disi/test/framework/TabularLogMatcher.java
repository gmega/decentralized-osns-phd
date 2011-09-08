package it.unitn.disi.test.framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.Assert;

import it.unitn.disi.utils.tabular.TableReader;

public class TabularLogMatcher {

	private final String[] fKeys;

	private ArrayList<String[]> fEvents = new ArrayList<String[]>();

	public TabularLogMatcher(String[] keys) {
		fKeys = keys;
	}

	public void add(Object... specs) {
		String[] specStr = new String[specs.length];
		for (int i = 0; i < specs.length; i++) {
			specStr[i] = specs[i].toString();
		}
		fEvents.add(specStr);
	}

	public void match(InputStream stream) throws IOException {
		TableReader reader = new TableReader(stream);
		Iterator<String[]> it = fEvents.iterator();
		while(it.hasNext()) {
			Assert.assertTrue(reader.hasNext());
			reader.next();
			String matching = matchEvent(it.next(), reader);
			Assert.assertTrue(matching, matching == null);
		}
		
		Assert.assertFalse(reader.hasNext());
	}

	private String matchEvent(String[] event, TableReader reader) {
		StringBuffer matching = new StringBuffer();
		matching.append("- ");
		for (int i = 0; i < fKeys.length; i++) {
			matching.append(reader.get(fKeys[i]));
			if (!reader.get(fKeys[i]).equals(event[i])) {
				matching.append("[");
				matching.append(event[i]);
				matching.append("]");
				return matching.toString();
			} else {
				matching.append(",");
			}
		}
		return null;
	}
}

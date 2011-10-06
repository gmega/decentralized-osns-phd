package it.unitn.disi.utils.tracetools;

import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.utils.tabular.TableReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.Assert;

import org.junit.Test;

public class AVTReplayTest {
	@Test
	public void testReplay() throws Exception {
		String AVT = "p1 1 0 3\n" + "p2 2 0 1 3 4\n " + "p3 2 1 3 5 5\n"
				+ "p4 2 1 1 4 5\n" + "p5 1 0 4\n";

		String ID_LIST = "p1\np2\np3\np4\np5\n";
		
		int [] REF_SIZES = new int [] {3, 5, 3, 4, 3, 2};
		int [] REF_IN = new int [] {3, 2, 0, 1, 1, 1};
		int [] REF_OUT = new int [] {0, 0, 2, 0, 2, 2};
 
		ByteArrayInputStream avt = new ByteArrayInputStream(AVT.getBytes());
		ByteArrayInputStream idList = new ByteArrayInputStream(
				ID_LIST.getBytes());

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		StreamProvider provider = new StreamProvider(new InputStream[] { avt,
				idList }, new OutputStream[] { out }, AVTReplay.class, false);
		
		AVTReplay replayer = new AVTReplay(1);

		replayer.execute(provider);
		
		TableReader reader = new TableReader(new ByteArrayInputStream(out.toByteArray()));
		for (int i = 0; i < REF_SIZES.length; i++) {
			reader.next();
			Assert.assertEquals(REF_SIZES[i], toInt(reader.get("size")));
			Assert.assertEquals(REF_IN[i], toInt(reader.get("in")));
			Assert.assertEquals(REF_OUT[i], toInt(reader.get("out")));
		}	
	}
	
	private int toInt(String str) {
		return Integer.parseInt(str);
	}
}

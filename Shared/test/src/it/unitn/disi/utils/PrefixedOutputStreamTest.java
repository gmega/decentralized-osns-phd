package it.unitn.disi.utils;

import it.unitn.disi.utils.streams.PrefixedWriter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import junit.framework.Assert;

import org.junit.Test;

public class PrefixedOutputStreamTest {

	static final String nl = System.getProperty("line.separator");

	@Test
	public void tagsSimpleLine() {
		ByteArrayOutputStream base = new ByteArrayOutputStream();
		PrintWriter normal = new PrintWriter(base);
		PrintWriter tagged = new PrintWriter(new PrefixedWriter("Well, and ",
				normal));

		normal.println("I think you are a jerk.");
		tagged.println("I think you are a jerk.");

		tagged.flush();

		String results = base.toString();
		StringBuffer richDialog = new StringBuffer();
		richDialog.append("I think you are a jerk.");
		richDialog.append(nl);
		richDialog.append("Well, and I think you are a jerk.");
		richDialog.append(nl);

		Assert.assertEquals(richDialog.toString(), results);
	}

	@Test
	public void tagsMultipleLines() throws IOException {

		ByteArrayOutputStream base = new ByteArrayOutputStream();
		PrintWriter normal = new PrintWriter(base);
		PrintWriter tagged = new PrintWriter(new PrefixedWriter("L:", normal));

		StringBuffer buf = new StringBuffer();
		buf.append("Line 1");
		buf.append(nl);
		buf.append("Line 2");
		buf.append(nl);
		buf.append("Line 3");
		buf.append(nl);
		buf.append("Line 4");
		buf.append(nl);
		buf.append("Line 5");

		tagged.append(buf);
		tagged.flush();

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(base.toByteArray())));
		
		for (int i = 1; i <= 5; i++) {
			String line = reader.readLine();
			Assert.assertEquals("L:Line " + i, line);
		}
		
		Assert.assertTrue(reader.readLine() == null);
	}
}

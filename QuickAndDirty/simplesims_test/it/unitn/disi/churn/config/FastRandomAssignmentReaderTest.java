package it.unitn.disi.churn.config;

import gnu.trove.list.array.TDoubleArrayList;

import it.unitn.disi.utils.MiscUtils;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

public class FastRandomAssignmentReaderTest {

	private static final double SAMPLES = 0.1;

	@Test
	public void testRandomReads() throws Exception {
		URL url = this.getClass().getClassLoader()
				.getResource("yao-assignments.bin");

		File file = new File(url.toURI());
		double[] reference = load(file);
		
		// Makes sure we don't have incomplete records in the file.
		Assert.assertEquals(0, reference.length % 2);
		
		FastRandomAssignmentReader reader = new FastRandomAssignmentReader(file);
		Random random = new Random();
		
		for (int i = 0; i < (reference.length * SAMPLES); i++) {
			int idx = random.nextInt(reference.length / 2);
			System.err.println(idx);
			reader.select(idx);

			Assert.assertEquals(reference[2 * idx], reader.li());
			Assert.assertEquals(reference[2 * idx + 1], reader.di());
		}
	}

	private double[] load(File file) throws IOException{
		TDoubleArrayList data = new TDoubleArrayList();
		RandomAccessFile reader = null;
		try {
			reader = new RandomAccessFile(file, "r");
			while(true) {
				data.add(reader.readDouble());
			}
		} catch (EOFException ex) {
			// Not a great example of how to catch an EOF...
		} finally {
			MiscUtils.safeClose(reader, true);
		}
		
		return data.toArray();
	}
}

package it.unitn.disi.utils.streams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

import junit.framework.Assert;


public class ResettableFileInputStreamTest {
	
	private static final int TRIALS = 100;
	
	private final Random random = new Random();
	
	@Test
	public void testMarkReset() throws Exception {
		URL url = ResettableFileInputStreamTest.class.getClassLoader()
				.getResource("charstream.text");
		ResettableFileInputStream stream = new ResettableFileInputStream(
				new File(url.toURI()));
		
		ByteArrayOutputStream contents = new ByteArrayOutputStream();
		
		// Reads bytes one by one.
		while(stream.available() > 0) {
			contents.write(stream.read());
		}
		
		Assert.assertTrue(stream.read() == -1);
		byte [] arrayContents = contents.toByteArray();
		
		// Perform 100 random passes.
		for (int i = 0; i < TRIALS; i++) {
			stream.fromZero();
			int markPoint = random.nextInt(arrayContents.length);
			
			assertSame(stream, arrayContents, 0, Math.max(0, markPoint - 1));
			
			stream.mark(Integer.MAX_VALUE);
			
			assertSame(stream, arrayContents, markPoint, arrayContents.length - 1);
			Assert.assertTrue(stream.read() == -1);
			
			stream.reset();
			
			assertSame(stream, arrayContents, markPoint, arrayContents.length - 1);
			Assert.assertTrue(stream.read() == -1);
		}
		
	}
	
	private void assertSame(InputStream is, byte[] reference, int start, int end)
			throws Exception {
		for (int i = start; i <= end; i++) {
			Assert.assertEquals(reference[i], is.read());
		}
	}
}

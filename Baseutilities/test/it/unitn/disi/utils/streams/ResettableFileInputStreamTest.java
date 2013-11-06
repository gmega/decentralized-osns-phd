package it.unitn.disi.utils.streams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

import junit.framework.Assert;


public class ResettableFileInputStreamTest {
	
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
		
		// Try all mark/reset positions.
		for (int markPoint = 1; markPoint < arrayContents.length; markPoint++) {
			stream.fromZero();
			
			assertSame(stream, arrayContents, 0, markPoint - 1);
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
			Assert.assertEquals("[" + start + ", " + end + ", " + i + "]", reference[i], is.read());
		}
	}
}

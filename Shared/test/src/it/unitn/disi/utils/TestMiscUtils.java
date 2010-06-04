package it.unitn.disi.utils;

import it.unitn.disi.utils.logging.CodecUtils;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

public class TestMiscUtils {
	@Test 
	public void testEncDec() {
		byte [] buf = new byte[4];
		
		for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE; i += 890123) {
			CodecUtils.encode(i, buf);
			Assert.assertEquals(i, CodecUtils.decodeInt(buf));
		}
	}
	
	@Test
	public void testEncLong() {
		byte [] buf = new byte[8];
		Random r = new Random();
		for (int i = 0; i < 10000; i++) {
			long num = r.nextLong();
			CodecUtils.encode(num, buf, 0);
			Assert.assertEquals(num, CodecUtils.decodeLong(buf, 0));
		}
	}
	
	@Test
	public void testAppendEncoding() {
		byte [] buf = new byte[96 + 128];
		
		long l1 = 8921319021L;
		long l2 = 789249789345L;
		
		int a = 123123123;
		int b = 812738923;
		int c = 1902832;
		
		int [] len = new int[4];
		
		len[0] = CodecUtils.append(l2, buf, 0);
		len[1] = CodecUtils.append(a, buf, len[0]);
		len[2] = CodecUtils.append(b, buf, len[1]);
		len[3] = CodecUtils.append(l1, buf, len[2]);
		CodecUtils.append(c, buf, len[3]);
		
		Assert.assertEquals(l2, CodecUtils.decodeLong(buf, 0));
		Assert.assertEquals(a, CodecUtils.decodeInt(buf, len[0]));
		Assert.assertEquals(b, CodecUtils.decodeInt(buf, len[1]));
		Assert.assertEquals(l1, CodecUtils.decodeLong(buf, len[2]));
		Assert.assertEquals(c, CodecUtils.decodeInt(buf, len[3]));
		
	}
}

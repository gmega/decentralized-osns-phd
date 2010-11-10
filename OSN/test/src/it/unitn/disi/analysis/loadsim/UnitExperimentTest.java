package it.unitn.disi.analysis.loadsim;

import junit.framework.Assert;

import org.junit.Test;

public class UnitExperimentTest {
	@Test
	public void testComputesNeighbourhoods() {
		UnitExperiment exp = new UnitExperiment(1, 5, false);
		
		exp.addData(10, 10, 10);
		exp.addData(2, 2, 2);
		exp.addData(4, 4, 4);
		
		exp.addData(10, 10, 10);
		exp.addData(6, 6, 6);
		exp.addData(2, 2, 2);
		exp.addData(8, 8, 8);
		exp.addData(4, 4, 4);
		
		exp.done();
		
		Assert.assertEquals(2, exp.messagesReceived(2, 0));
		Assert.assertEquals(2, exp.messagesReceived(2, 1));
		
		Assert.assertEquals(4, exp.messagesReceived(4, 0));
		Assert.assertEquals(4, exp.messagesReceived(4, 1));
		
		Assert.assertEquals(0, exp.messagesReceived(6, 0));
		Assert.assertEquals(6, exp.messagesReceived(6, 1));
		
		Assert.assertEquals(0, exp.messagesReceived(8, 0));
		Assert.assertEquals(8, exp.messagesReceived(8, 1));
		
		Assert.assertEquals(10, exp.messagesReceived(10, 0));
		Assert.assertEquals(10, exp.messagesReceived(10, 1));
	}
	
	@Test
	public void testCumulativeWorks() {
		UnitExperiment exp = new UnitExperiment(0, 3, true);
		
		exp.addData(0, 1, 2);
		exp.addData(1, 0, 0);
		exp.addData(2, 1, 1);
		
		exp.addData(0, 1, 2);
		exp.addData(1, 1, 1);
		exp.addData(2, 13, 25);
		
		try {
			exp.addData(2, 11, 25);
			Assert.fail();
		} catch (IllegalStateException ex) { }
		
		try {
			exp.addData(0, 1, 0);
			Assert.fail();
		} catch (IllegalStateException ex) { }
		
		exp.addData(0, 2, 2);
		exp.addData(1, 1, 3);
		exp.addData(2, 13, 26);
		
		exp.done();
		
		assertSent(exp, 0, 1, 0, 1);
		assertRecv(exp, 0, 2, 0, 0);
		
		assertSent(exp, 1, 0, 1, 0);
		assertRecv(exp, 1, 0, 1, 2);
		
		assertSent(exp, 2, 1, 12, 0);
		assertRecv(exp, 2, 1, 24, 1);
	}	
	
	private void assertSent(UnitExperiment exp, int id, int...vals) {
		for (int i = 0; i < vals.length; i++) {
			Assert.assertEquals(exp.messagesSent(id, i), vals[i]);
		} 
	}
	
	private void assertRecv(UnitExperiment exp, int id, int...vals) {
		for (int i = 0; i < vals.length; i++) {
			Assert.assertEquals(exp.messagesReceived(id, i), vals[i]);
		} 
	}
}

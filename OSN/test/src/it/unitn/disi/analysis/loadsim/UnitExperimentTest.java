package it.unitn.disi.analysis.loadsim;

import junit.framework.Assert;

import org.junit.Test;

public class UnitExperimentTest {
	@Test
	public void testComputesNeighbourhoods() {
		UnitExperiment exp = new UnitExperiment(1, 5);
		
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
}

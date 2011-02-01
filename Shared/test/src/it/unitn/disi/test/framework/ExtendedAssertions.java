package it.unitn.disi.test.framework;

import junit.framework.Assert;

public class ExtendedAssertions {
	
	public static <T> void assertContains(T element, T [] array) {
		for (int i = 0; i < array.length; i++) {
			if (element.equals(array[i])) {
				return;
			}
		}
		
		Assert.fail("Element " + element.toString() + " not found.");
	}

}

package it.unitn.disi;

import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.CompactEventStorage;

import java.util.Vector;

import peersim.core.Node;

public class MemTest {
	public static void main(String [] args) {
		Vector<CompactEventStorage> v = new Vector<CompactEventStorage>();
		
		int i = 0;
		Node n = TestUtils.makeNode();
		try{
			while (true) {
				v.add(new CompactEventStorage());
				i++;
			}
		} catch(OutOfMemoryError err) {
			v = null;
			System.err.println(i);
		}
	}
}

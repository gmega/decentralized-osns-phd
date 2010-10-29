package it.unitn.disi.f2f;

import java.util.BitSet;

import peersim.extras.am.epidemic.AbstractMessage;

public class Digest extends AbstractMessage {
	public final BitSet known = new BitSet();
	public final BitSet wanted = new BitSet();
}

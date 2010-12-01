package it.unitn.disi.graph.lightweight;

import it.unitn.disi.graph.codecs.ResettableGraphDecoder;

public class LSGLoader extends LSGCreator {

	private final ResettableGraphDecoder fDecoder;

	public LSGLoader(ResettableGraphDecoder decoder) {
		fDecoder = decoder;
	}

	@Override
	protected void graphLoop(Action action) throws Exception {
		fDecoder.reset();
		while (fDecoder.hasNext()) {
			int source = fDecoder.getSource();
			int target = fDecoder.next();
			action.innerAction(source, target);
		}
	}
}

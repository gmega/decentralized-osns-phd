package it.unitn.disi.graph.lightweight;

public abstract class LSGTransformer extends LSGCreator {
	
	private LightweightStaticGraph fSource;
	
	public LightweightStaticGraph transform(LightweightStaticGraph source) {
		fSource = source;
		return create();
	}

	public LightweightStaticGraph sourceGraph() {
		if (fSource == null) {
			throw new NullPointerException();
		}
		return fSource;
	}
	
	public void freeSource() {
		fSource = null;
	}
}

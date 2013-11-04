package it.unitn.disi.graph.large.catalog;

public abstract class AbstractCatalogCursor implements ICatalogCursor{
	
	private final ICatalogRecordType fType;
	
	private Number [] fValueBuffer;
	
	public AbstractCatalogCursor(ICatalogRecordType type, boolean includeMagic) {
		fType = type;
		fValueBuffer = new Number[type.getParts().size() + (includeMagic ? 1 : 0)];
	}
	
	protected abstract boolean isReady();
	
	protected ICatalogRecordType type() {
		return fType;
	}
	
	protected Number [] valueBuffer() {
		return fValueBuffer;
	}
	
	@Override
	public Number get(String key) {
		if (!isReady()) {
			throw new IllegalStateException();
		}
		return get(fType.indexOf(key));
	}
	
	public Number get(int index) {
		if (index < 0 || index >= fValueBuffer.length) {
			throw new IllegalArgumentException();
		}
		return fValueBuffer[index];
	}

}

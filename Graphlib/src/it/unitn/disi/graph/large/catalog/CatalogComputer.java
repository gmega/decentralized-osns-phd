package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;
import java.util.List;
import java.util.NoSuchElementException;

public class CatalogComputer extends AbstractCatalogCursor {

	private final ICatalogRecordType fType;

	private final IndexedNeighborGraph fGraph;
	private int fCurrent = -1;

	public CatalogComputer(IndexedNeighborGraph graph, ICatalogRecordType type) {
		super(type, false);
		fType = type;
		fGraph = graph;
	}

	@Override
	public boolean hasNext() {
		return fCurrent < (fGraph.size() - 1);
	}

	@Override
	public void next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		
		List<ICatalogPart<? extends Number>> parts = fType.getParts();
		fCurrent++;
		for (int i = 0; i < parts.size(); i++) {
			ICatalogPart<? extends Number> part = parts.get(i);
			valueBuffer()[i] = part.compute(fGraph, fCurrent);
		}
	}

	@Override
	protected boolean isReady() {
		return fCurrent > 0;
	}
	
	public int currentNeighborhood() {
		return fCurrent;
	}

}

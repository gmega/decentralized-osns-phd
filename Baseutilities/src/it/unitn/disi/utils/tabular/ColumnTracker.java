package it.unitn.disi.utils.tabular;

/**
 * Simple utility class for helping track changing row values on tables with
 * multiple repeated elements.
 * 
 * @author giuliano
 */
public class ColumnTracker {

	private TableReader fReader;

	private String fCurrent;

	private String fId;

	public ColumnTracker(TableReader reader, String idfield) {
		fReader = reader;
		fId = idfield;
	}

	public boolean hasChanged() {
		String current = fReader.get(fId);
		if (!current.equals(fCurrent)) {
			fCurrent = current;
			return true;
		}

		return false;
	}
	
	public String current() {
		return fCurrent;
	}
}

package it.unitn.disi.utils.tabular;


public class NullTableWriter implements ITableWriter {

	@Override
	public boolean set(String key, String value) {
		return true;
	}

	@Override
	public boolean set(String key, int value) {
		return true;
	}

	@Override
	public boolean set(String key, long value) {
		return true;
	}

	@Override
	public boolean set(String key, double value) {
		return true;
	}

	@Override
	public boolean set(String key, float value) {
		return true;
	}
	
	@Override
	public boolean set(String key, boolean value) {
		return true;
	}

	@Override
	public boolean set(String key, Object object) {
		return true;
	}

	@Override
	public void newRow() {
	}

	@Override
	public void emmitRow() {
	}
	
	@Override
	public String[] fields() {
		return new String [] {};
	}

}

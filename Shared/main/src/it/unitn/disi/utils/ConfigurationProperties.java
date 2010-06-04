package it.unitn.disi.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class ConfigurationProperties {
	
	private final Map<String, String> fProps;
	
	public ConfigurationProperties(Map<String, String> props) {
		fProps = props == null ? new HashMap<String, String>() : props;
	}
	
	public String get(String property) {
		return fProps.get(property);
	}
	
	public Set<String> validate(Set<String> keys) {
		Set <String> missing = new HashSet<String>();
		
		for (String key : keys) {
			if(!fProps.containsKey(key)) {
				missing.add(key);
			}
		}
		
		return missing;
	}
}

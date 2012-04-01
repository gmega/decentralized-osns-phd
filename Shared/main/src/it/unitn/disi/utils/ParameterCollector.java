package it.unitn.disi.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ParameterCollector {

	public static class ParameterizedFile {
		public final Map<String, String> attributes;

		public final File file;

		public ParameterizedFile(File file, Map<String, String> attributes) {
			this.attributes = attributes;
			this.file = file;
		}
	}

	private static final String IDENT = "[A-Z]+";
	private static final String INT_OR_FLOAT = "[0-9]+(?:\\.[0-9]+)?";
	private static final String INT_OR_FLOAT_OR_IDENT = "(?:" + IDENT + "|(?:"
			+ INT_OR_FLOAT + "))";
	private static final String SPLIT_CHAR = "_";
	private static final String PARAMETER = IDENT + SPLIT_CHAR
			+ INT_OR_FLOAT_OR_IDENT;
	
	private static final Pattern pat = Pattern.compile(PARAMETER);

	public List<ParameterizedFile> collect(File folder, String radix,
			String extension) throws IOException {
		folder.list();
		return null;
	}
}

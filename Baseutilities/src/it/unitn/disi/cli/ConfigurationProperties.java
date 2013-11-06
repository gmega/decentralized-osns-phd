package it.unitn.disi.cli;

import it.unitn.disi.utils.exception.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.StringValueResolver;

/**
 * Simple configuration property manager which supports a three-level lookup
 * structure, with the second level being defined by a key. The main idea is
 * allowing users to organize simulation properties in three levels, where:
 * 
 * <ol>
 * <li>the first level contains common simulation parameters;</li>
 * <li>the second level contains protocol-specific variations (e.g. parameters);
 * </li>
 * <li>the third level comes from the command line.</li>
 * </ol>
 * 
 * Different {@link IResolver}s can then be constructed by varying the key when
 * calling {@link #resolver(String)}.
 * 
 * @author giuliano
 */
public class ConfigurationProperties {

	private static enum TokenType {
		load, section_decl, section_end, comment, attribute, empty_line
	}

	public static final String ROOT_SECTION = "root";

	public static final String TOP_SECTION = "top";

	/** Pattern for matching bash-style variables. */
	private static final Pattern fVarPattern = Pattern
			.compile("\\$\\{(.*?)\\}");

	private HashMap<String, Properties> fProperties = new HashMap<String, Properties>();

	private String fSection = ROOT_SECTION;

	private File fBase = new File(".");

	public ConfigurationProperties() {
		fProperties.put(ROOT_SECTION, new Properties());
		fProperties.put(TOP_SECTION, new Properties());
	}

	public void setProperty(String key, String value) {
		fProperties.get(TOP_SECTION).put(key, value);
	}

	public void load(File file) throws IOException {
		FileInputStream iStream = null;
		try {
			iStream = new FileInputStream(file);
			// XXX Not really the best way to do this.
			fBase = file.getAbsoluteFile().getParentFile();
			load(iStream);
		} finally {
			if (iStream != null) {
				iStream.close();
			}
		}
	}

	public void load(InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));

		parse(reader);
	}

	public IResolver resolver(String section) {
		
		Properties sectionProps = fProperties.get(section);
		if (sectionProps == null) {
			return null;
		}
		
		final Properties merged = new Properties();

		merged.putAll(fProperties.get(ROOT_SECTION));
		merged.putAll(sectionProps);
		merged.putAll(fProperties.get(TOP_SECTION));

		for (Object key : merged.keySet()) {
			merged.put(key, expand(merged.getProperty((String) key), merged));
		}

		return new StringValueResolver() {
			@Override
			public String getString(String prefix, String key) {
				String value = merged.getProperty(key);
				if (value == null) {
					throw new MissingParameterException(key);
				}
				return value;
			}
		};
	}

	private void parse(BufferedReader reader) throws IOException {

		String line;
		while ((line = reader.readLine()) != null) {
			switch (lineType(line)) {

			case load:
				load(file(line));
				break;

			case section_decl:
				sectionStart(line);
				break;

			case section_end:
				sectionEnd();
				break;

			case attribute:
				parseAttribute(line);

			case empty_line:
			case comment:
				break;

			}
		}

	}

	private void parseAttribute(String line) {
		String[] attribute = line.split("=");
		if (attribute.length != 2) {
			throw new ParseException("Malformed attribute " + line + ".");
		}
		fProperties.get(fSection).put(attribute[0].trim(), attribute[1].trim());
	}

	private void sectionEnd() {
		fSection = ROOT_SECTION;
	}

	private File file(String line) {
		return new File(fBase, line.substring(1, line.length() - 1));
	}

	private void sectionStart(String rawline) {
		if (fSection != ROOT_SECTION) {
			throw new ParseException("Nested sections are not supported.");
		}

		String[] sectionDecl = rawline.split(" ");
		if (sectionDecl.length != 3 || !sectionDecl[2].equals("{")) {
			throw new ParseException("Malformed section declaration " + rawline
					+ ".");
		}

		fSection = sectionDecl[1].trim();
		Properties sectionProperties = new Properties();
		sectionProperties.put("section", fSection);
		fProperties.put(fSection, sectionProperties);
	}

	private String expand(String string, Properties props) {
		Matcher matcher = fVarPattern.matcher(string);
		StringBuffer moldable = new StringBuffer(string);

		while (matcher.find()) {
			String key = matcher.group(1);
			String value;
			// Lookup properties.
			if (props.containsKey(key)) {
				value = props.get(key).toString();
			}
			// Lookup system var.
			else {
				value = System.getenv(key);
			}

			if (value == null) {
				throw new ParseException("Can't resolve variable " + key + ".");
			}

			moldable.replace(matcher.start(0), matcher.end(0), value);
		}
		return moldable.toString();
	}

	private TokenType lineType(String line) {
		// Poor man's lexer.
		if (line.startsWith("section")) {
			return TokenType.section_decl;
		}

		if (line.startsWith("}")) {
			return TokenType.section_end;
		}

		if (line.startsWith("[")) {
			return TokenType.load;
		}

		if (line.startsWith("#")) {
			return TokenType.comment;
		}

		if (line.trim().equals("")) {
			return TokenType.empty_line;
		}

		return TokenType.attribute;
	}
}

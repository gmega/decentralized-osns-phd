package it.unitn.disi.cli;

import groovy.lang.GroovyShell;
import it.unitn.disi.utils.SectionReplacer;
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
 * Simple (and not really robust) configuration property manager which supports
 * a three-level lookup structure, with the second level being defined by a key.
 * The main idea is allowing users to organize simulation properties in three
 * levels, where:
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
		load, section_decl, section_end, comment, attribute, empty_line, section_include
	}

	public static final String ROOT_SECTION = "root";

	public static final String TOP_SECTION = "top";

	/** Pattern for matching bash-style variables. */
	private static final Pattern fVarPattern = Pattern
			.compile("\\{(.*?)\\}|(\\$\\$)");

	/** Assigns symbolic names to pattern groups to make code more readable. */
	private static final int VAR_KEY = 1;
	private static final int NO_EXPAND = 2;

	/** Pattern for matching scripts. */
	private static final Pattern fScriptPattern = Pattern.compile(
			"\\s*script\\s*(!?\\{)(.*?)\\}", Pattern.DOTALL);

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
		// We don't care about closing this reader cause the
		// stream isn't ours.
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
			line = line.trim();
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

			case section_include:
				includeSection(line);
				break;

			case attribute:
				parseAttribute(line, reader);

			case empty_line:
			case comment:
				break;

			}
		}

	}

	private void includeSection(String line) {
		String[] section = line.split(" ");
		if (section.length != 2) {
			throw new ParseException("Invalid use clause " + line + ".");
		}

		Properties props = checkedGet(section[1].trim());
		if (props == null) {
			throw new ParseException("Invalid use clause " + line
					+ " -- section " + section[1] + " is not yet defined.");
		}

		fProperties.get(fSection).putAll(props);
	}

	public Properties checkedGet(String section) {
		return fProperties.get(section);
	}

	private String script(Matcher matcher) throws IOException {
		String evalMode = matcher.group(1);
		String script = matcher.group(2);

		if (evalMode.startsWith("!")) {
			return script;
		}

		// For now, uses raw environment.
		GroovyShell shell = new GroovyShell();
		Object val = shell.evaluate(script);

		return (val == null) ? null : val.toString();
	}

	private void parseAttribute(String line, BufferedReader reader)
			throws IOException {
		String[] attribute = line.split("=");
		if (attribute.length != 2) {
			throw new ParseException("Malformed attribute " + line + ".");
		}

		// Checks if it's a script.
		Matcher m = fScriptPattern.matcher(attribute[1]);
		if (m.matches()) {
			script(m);
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
		SectionReplacer replacer = new SectionReplacer(string);

		while (matcher.find()) {
			String value;
			if (matcher.group(NO_EXPAND) != null) {
				value = "$";
			} else {
				value = varLookup(props, matcher.group(VAR_KEY));
			}
			replacer.replace(matcher.start(0), matcher.end(0) - 1, value);
		}

		return replacer.toString();
	}

	public String varLookup(Properties props, String key) throws ParseException {
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
		return value;
	}

	private TokenType lineType(String line) {
		// Poor man's lexer.
		if (line.startsWith("section")) {
			return TokenType.section_decl;
		}

		if (line.startsWith("use")) {
			return TokenType.section_include;
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

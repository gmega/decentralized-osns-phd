package it.unitn.disi.logparse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.BaseFormatDecoder;
import it.unitn.disi.utils.collections.Pair;
import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Java version of the script for extracting matching lines from huge log files.
 * 
 * @author giuliano
 */
@AutoConfig
public class LogParse implements ITransformer {

	private static final Logger fLogger = Logger.getLogger(LogParse.class);

	@Attribute("file_list")
	private String fFileList;

	@Attribute("line_prefix")
	private String fLinePrefix;

	@Attribute("matching_only")
	private boolean fMatchingOnly;
	
	private String[] fParameters;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		String[] list = fFileList.split(" ");
		for (int i = 0; i < list.length; i++) {
			fLogger.info("File " + (i + 1) + " of " + list.length + ".");
			String file = list[i];
			Pair<String, String> pars = parameters(file);
			if (pars != null) {
				processFile(file, pars.a, pars.b, new PrintStream(oup));
			}
		}
	}

	private void processFile(String filename, String substring,
			String replacement, PrintStream out) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				BaseFormatDecoder.open(new File(filename))));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith(fLinePrefix)) {
				// Performs the replacement.
				line = substitute(fLinePrefix, substring, replacement, line);
			} else if (fMatchingOnly) {
				continue;
			}
			out.println(line);
		}
	}

	private String substitute(String prefix, String substring,
			String replacement, String line) {
		line = line.substring(prefix.length());
		String[] split = line.split(substring);
		if (split.length == 2) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(split[0].trim());
			buffer.append(" ");
			buffer.append(replacement);
			buffer.append(" ");
			buffer.append(split[1].trim());
			line = buffer.toString().trim();
		} else {
			line = split[0];
		}
		return line;
	}

	private Pair<String, String> parameters(String file) {
		ParameterIterator matcher = new ParameterIterator(file);
		if (fParameters == null) {
			fParameters = extract(matcher);
			matcher = new ParameterIterator(file);
		}

		StringBuffer parString = new StringBuffer();
		StringBuffer replacementString = new StringBuffer();
		Pair<String, String> par;
		int count = 0;
		while ((par = matcher.next()) != null) {
			String key = par.a;
			String value = par.b;
			if (!fParameters[count].equals(key)) {
				fLogger.warn(String.format(
						"Match failure on %1$s: expected %1$s but got %1$s.",
						fParameters[count], key));
			}
			parString.append(key);
			parString.append(" ");
			parString.append(value);
			parString.append(" ");
			replacementString.append(value);
			replacementString.append(" ");
			count++;
		}

		fLogger.info("Looking for: " + parString);

		if (count != fParameters.length) {
			fLogger.warn(String
					.format("Match failure on %1$s: expected %2$d parameters but found %3$d.",
							file, fParameters.length, count));
			return null;
		}

		replacementString.deleteCharAt(replacementString.length() - 1);
		parString.deleteCharAt(parString.length() - 1);
		return new Pair<String, String>(parString.toString(),
				replacementString.toString());
	}

	private String[] extract(ParameterIterator iterator) {
		ArrayList<String> parTemp = new ArrayList<String>();
		StringBuffer buffer = new StringBuffer("(");
		Pair<String, String> element;
		while ((element = iterator.next()) != null) {
			parTemp.add(element.a);
			buffer.append(element.a);
			buffer.append(", ");
		}

		if (buffer.length() > 1) {
			buffer.delete(buffer.length() - 2, buffer.length());
		}
		buffer.append(")");

		fLogger.info("Parameters are: " + buffer.toString() + ".");
		return parTemp.toArray(new String[parTemp.size()]);
	}

}

class ParameterIterator {
	private static final String IDENT = "[A-Z]+";
	private static final String INT_OR_FLOAT = "[0-9]+(?:\\.[0-9]+)?";
	private static final String SPLIT_CHAR = "_";
	private static final String PARAMETER = IDENT + SPLIT_CHAR + INT_OR_FLOAT;

	private static final Pattern fPattern = Pattern.compile(PARAMETER);

	private final Matcher fMatcher;

	public ParameterIterator(String s) {
		fMatcher = fPattern.matcher(s);
	}

	public Pair<String, String> next() {
		if (!fMatcher.find()) {
			return null;
		}
		String next = fMatcher.group();
		String[] match = next.split(SPLIT_CHAR);
		return new Pair<String, String>(match[0], match[1]);
	}

}

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
 * {@link PeerSimLogDemux} can "demultiplex" PeerSim log files. It's been
 * designed to be fast, to cope with PeerSim quirks, and to understand certain
 * naming conventions that make handling logs easier.
 * 
 * XXX This script was coded in a fast-and-furious way, but by now it is
 * accumulating too much functionality, and should be refactored.
 * 
 * @author giuliano
 */
@AutoConfig
public class PeerSimLogDemux implements ITransformer {

	private static final Logger fLogger = Logger
			.getLogger(PeerSimLogDemux.class);

	/**
	 * A list of files to parse. Might be gzipped or plain text files.
	 */
	@Attribute("file_list")
	private String fFileList;

	/**
	 * An invariant prefix which identifies lines of interest.
	 */
	@Attribute("line_prefix")
	private String fLinePrefix;

	/**
	 * If true, only outputs lines with the matching prefix.
	 */
	@Attribute("matching_only")
	private boolean fMatchingOnly;

	/**
	 * If true, allows partially completed files to be parsed.
	 */
	@Attribute("allow_partial")
	private boolean fAllowPartial;

	/**
	 * If true, assumes that the files being merged contain headers.<BR>
	 * FIXME This is broken unless 'discard_parameters' is set to true. I need
	 * to properly implement it for the case when the original header is to be
	 * extended.
	 */
	@Attribute("single_header")
	private boolean fSingleHeader;

	/**
	 * If true, causes the parameter string to be completely removed from the
	 * log file (not just reformatted).
	 */
	@Attribute("discard_parameters")
	private boolean fDiscardParameters;

	private String[] fParameters;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		String[] list = fFileList.split(" ");
		for (int i = 0; i < list.length; i++) {
			fLogger.info("File " + (i + 1) + " of " + list.length + ".");
			String file = list[i];
			Pair<String, String> pars = parameters(file);
			if (pars != null) {
				try {
					processFile(file, pars.a, pars.b, new PrintStream(oup),
							i != 0 && fSingleHeader, !pars.a.equals(""));
				} catch (java.io.EOFException ex) {
					fLogger.error("Unexpected end-of-file found.");
					if (!fAllowPartial) {
						fLogger.error("Re-run with the allow_partial option to skip this file and continue.");
						return;
					}
				}
			}
		}
	}

	private void processFile(String filename, String substring,
			String replacement, PrintStream out, boolean singleHeader,
			boolean replace) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				BaseFormatDecoder.open(new File(filename))));
		String line = null;
		boolean first = true;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith(fLinePrefix)) {
				if (replace) {
					// Performs the replacement.
					line = substitute(fLinePrefix, substring, replacement, line);
				}
			} else if (fMatchingOnly) {
				continue;
			}

			if (!first || !singleHeader) {
				out.println(line);
			}

			first = false;
		}
	}

	private String substitute(String prefix, String substring,
			String replacement, String line) {
		line = line.substring(prefix.length());
		String[] split = line.split(substring);
		split[0] = split[0].trim();
		if (split.length == 2) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(split[0]);
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

		if (parString.length() == 0) {
			fLogger.info("No parameters in file " + file + ".");
		} else {
			fLogger.info("Looking for: " + parString);
			if (count != fParameters.length) {
				fLogger.warn(String
						.format("Match failure on %1$s: expected %2$d parameters but found %3$d.",
								file, fParameters.length, count));
				return null;
			}
			replacementString.deleteCharAt(replacementString.length() - 1);
			parString.deleteCharAt(parString.length() - 1);
		}

		return new Pair<String, String>(parString.toString(),
				fDiscardParameters ? "" : replacementString.toString());
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
	private static final String INT_OR_FLOAT_OR_IDENT = "(?:" + IDENT + "|(?:" + INT_OR_FLOAT + "))";
	private static final String SPLIT_CHAR = "_";
	private static final String PARAMETER = IDENT + SPLIT_CHAR + INT_OR_FLOAT_OR_IDENT;

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

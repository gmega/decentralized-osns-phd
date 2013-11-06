package it.unitn.disi.utils.tracetools;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.tabular.ITableWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import peersim.config.AutoConfig;

/**
 * Reads a file in <a href="http://www.eurecom.fr/~btroup/kadtraces/">zone crawl
 * format</a> and prints a table with:<BR>
 * <code>
 * [peer id] [country] [time of first login (start)] [time of last logout (end)]<BR>
 * </code>
 * 
 * @author giuliano
 */
@AutoConfig
public class KADLifetimes implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		ITableWriter writer = new TableWriter(new PrintStream(oup),
				new String[] { "id", "country", "start", "end" });

		String line;
		while ((line = reader.readLine()) != null) {
			StringTokenizer strtok = new StringTokenizer(line);
			String id = strtok.nextToken();
			String country = strtok.nextToken();

			int firstLogin = 0;
			int lastLogout = 0;
			int i;
			
			for (i = 0; strtok.hasMoreTokens(); i++) {
				String token = strtok.nextToken();
				if (token.equals("1")) {
					firstLogin = lastLogout = i;
					break;
				}
			}

			for (; strtok.hasMoreTokens(); i++) {
				String token = strtok.nextToken();
				if (token.equals("1")) {
					lastLogout = i;
				}
			}

			writer.set("id", id);
			writer.set("country", country);
			writer.set("start", firstLogin);
			writer.set("end", lastLogout);
			writer.emmitRow();
		}
	}

}

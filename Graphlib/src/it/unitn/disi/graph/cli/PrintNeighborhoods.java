package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.large.catalog.CatalogReader;
import it.unitn.disi.graph.large.catalog.CatalogRecordTypes;
import it.unitn.disi.graph.large.catalog.PartialLoader;
import it.unitn.disi.utils.tabular.TableReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.Attribute;

public class PrintNeighborhoods implements ITransformer {

	@Attribute("catalog")
	private String fCatalog;

	@Attribute("graph")
	private String fGraph;

	@Attribute("folder")
	private String fFolder;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		File outputFolder = new File(fFolder);

		PartialLoader loader = new PartialLoader(new CatalogReader(
				new FileInputStream(new File(fCatalog)),
				CatalogRecordTypes.PROPERTY_RECORD), ByteGraphDecoder.class,
				new File(fGraph));

		TableReader reader = new TableReader(is);
		while (reader.hasNext()) {
			reader.next();
			int root = Integer.parseInt(reader.get("id"));
			IndexedNeighborGraph graph = loader.subgraph(root);
			int[] ids = loader.verticesOf(root);

			String name = root + ".ncol";
			System.out.println("Write file [" + name + "]");

			PrintStream out = new PrintStream(new File(outputFolder, name));
			for (int i = 0; i < graph.size(); i++) {
				for (int j = 0; j < graph.degree(i); j++) {
					StringBuffer b = new StringBuffer();
					b.append(ids[i]);
					b.append(" ");
					b.append(ids[graph.getNeighbor(i, j)]);
					b.append(" 1");

					out.println(b);
				}
			}

			out.close();
		}
	}

}

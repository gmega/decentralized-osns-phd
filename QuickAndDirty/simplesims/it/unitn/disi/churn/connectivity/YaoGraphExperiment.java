package it.unitn.disi.churn.connectivity;

import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.large.catalog.CatalogReader;
import it.unitn.disi.graph.large.catalog.CatalogRecordTypes;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.large.catalog.PartialLoader;
import it.unitn.disi.network.churn.yao.AveragesFromFile;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.network.churn.yao.YaoPresets;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.unitsim.ListGraphGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.Configuration;

public class YaoGraphExperiment {
	
	static {
		Configuration.setConfig(new Properties());
	}

	@Attribute(value = "graph", defaultValue = "none")
	private String fGraph;

	@Attribute(value = "catalog", defaultValue = "none")
	private String fCatalog;

	@Attribute("graphtype")
	private String fGraphType;

	@Attribute(value = "yaomode")
	protected String fMode;

	@Attribute(value = "assignments", defaultValue = "yao")
	protected String fAssignments;

	protected IGraphProvider graphProvider() throws Exception {
		if (fGraphType.equals("catalog")) {
			CatalogReader reader = new CatalogReader(new FileInputStream(
					new File(fCatalog)), CatalogRecordTypes.PROPERTY_RECORD);
			System.err.print("-- Loading catalog...");
			PartialLoader loader = new PartialLoader(reader,
					ByteGraphDecoder.class, new File(fGraph));
			loader.start(null);
			System.err.println("done.");
			return loader;
		} else if (fGraphType.equals("linegraph")) {
			return new ListGraphGenerator();
		}
		
		return null;
	}

	protected IAverageGenerator averageGenerator() {
		if (fAssignments.toLowerCase().equals("yao")) {
			return YaoPresets.averageGenerator("yao");
		} else {
			return new AveragesFromFile(fAssignments, false);
		}
	}

	protected IDistributionGenerator distributionGenerator() {
		return YaoPresets.mode(fMode.toUpperCase(), new Random());
	}
}

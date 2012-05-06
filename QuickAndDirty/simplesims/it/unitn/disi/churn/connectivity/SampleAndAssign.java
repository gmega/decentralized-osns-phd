package it.unitn.disi.churn.connectivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.tabular.TableWriter;

/**
 * Selects an ego-centric network sample uniformly at random from the underlying
 * graph and generates a churn assignment for it. 
 * 
 * @author giuliano
 */
@AutoConfig
public class SampleAndAssign implements ITransformer {

	private GraphConfigurator fGraphConf;

	private YaoChurnConfigurator fYaoConf;

	@Attribute("samples")
	private int fSamples;

	public SampleAndAssign(@Attribute(Attribute.AUTO) IResolver resolver) {
		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);
		fGraphConf = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableWriter out = new TableWriter(oup, "id", "node", "li", "di");
		IGraphProvider provider = fGraphConf.graphProvider();
		
		int[] samples = generate(provider.size());
		IAverageGenerator gen = fYaoConf.averageGenerator();

		for (int i = 0; i < fSamples; i++) {
			int root = samples[i];
			int[] ids = provider.verticesOf(root);
			for (int j = 0; j < ids.length; j++) {
				out.set("id", root);
				out.set("node", ids[j]);
				out.set("li", gen.nextLI());
				out.set("di", gen.nextDI());
				out.emmitRow();
			}
		}
	}

	private int[] generate(int size) {
		int[] array = new int[size];
		for (int i = 0; i < array.length; i++) {
			array[i] = i;
		}
		OrderingUtils.permute(0, array.length, array, new Random());
		return array;
	}

}

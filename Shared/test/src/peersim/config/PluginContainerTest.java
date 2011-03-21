package peersim.config;

import java.io.File;
import java.net.URL;

import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestUtils;
import junit.framework.Assert;

import org.junit.Test;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.ConfigContainer;
import peersim.config.ParsedProperties;
import peersim.config.plugin.IPlugin;
import peersim.config.plugin.IPluginDescriptor;
import peersim.config.plugin.PluginContainer;

public class PluginContainerTest extends PeerSimTest {

	public static int count = 0;

	@Test
	public void testRespectsDependencies() throws Exception {
		URL conf = TestUtils.locate("plugintest.properties");
		ConfigContainer confContainer = new ConfigContainer(new ParsedProperties(
				new File(conf.toURI()).getAbsolutePath()), false);
		confContainer.getPluginContainer().start();
		PluginContainer container = confContainer.getPluginContainer();
		plAssert(container, "p3", 0);
		plAssert(container, "p1", 3);
		plAssert(container, "p2", 4);
	}
	
	@Test
	public void testDetectsCycles() throws Exception {
		URL conf = TestUtils.locate("plugincyclestest.properties");
		try {
			ConfigContainer confContainer = new ConfigContainer(new ParsedProperties(
					new File(conf.toURI()).getAbsolutePath()), false);
			confContainer.getPluginContainer().start();
			Assert.fail();
		} catch (IllegalStateException ex) {
			
		}
	}
	
	
	private void plAssert(PluginContainer container, String id, int activation) {
		BasePlugin plugin = (BasePlugin) container.getObject(null, id);
		Assert.assertTrue(plugin.id().equals(id));
		Assert.assertEquals(activation, plugin.getActivation());
	}
}

@AutoConfig
abstract class BasePlugin implements IPlugin {

	private int fActivation;

	private String fProp;

	public BasePlugin(IPluginDescriptor descriptor, String prop) {
		Assert.assertTrue(descriptor.id().equals(id()));
		fProp = prop;
	}

	@Override
	public void start(IResolver resolver) {
		fActivation = PluginContainerTest.count++;
	}

	@Override
	public void stop() {
	}

	public int getActivation() {
		return fActivation;
	}

	public String getProp() {
		return fProp;
	}

}

@AutoConfig
class P1 extends BasePlugin {

	public P1(@Attribute("p1.descriptor") IPluginDescriptor descriptor,
			@Attribute("prop") String property) {
		super(descriptor, property);
	}

	@Override
	public String id() {
		return "p1";
	}

}

@AutoConfig
class P2 extends BasePlugin {

	public P2(@Attribute("p2.descriptor") IPluginDescriptor descriptor,
			@Attribute("prop") String property) {
		super(descriptor, property);
	}

	@Override
	public String id() {
		return "p2";
	}
}

@AutoConfig
class P3 extends BasePlugin {

	public P3(@Attribute("p3.descriptor") IPluginDescriptor descriptor,
			@Attribute("prop") String property) {
		super(descriptor, property);
	}

	@Override
	public String id() {
		return "p3";
	}
}

@AutoConfig
class P4 extends BasePlugin {

	public P4(@Attribute("p4.descriptor") IPluginDescriptor descriptor,
			@Attribute("prop") String property) {
		super(descriptor, property);
	}

	@Override
	public String id() {
		return "p4";
	}
}

@AutoConfig
class P5 extends BasePlugin {

	public P5(@Attribute("p5.descriptor") IPluginDescriptor descriptor,
			@Attribute("prop") String property) {
		super(descriptor, property);
	}

	@Override
	public String id() {
		return "p5";
	}
}

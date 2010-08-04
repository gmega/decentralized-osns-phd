package peersim.config;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

import it.unitn.disi.TestUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class ObjectCreatorTest {

	@BeforeClass
	public static void loadConfiguration() throws Exception {
		URL fileURL = TestUtils.locate("creator_test_config.properties");
		File f = new File(fileURL.toURI());
		Configuration.setConfig(new ParsedProperties(f.getAbsolutePath()));
	}
	
	@Test
	public void simpleFieldInjection() throws Exception {
		ObjectCreator<FieldInjectable> creator = new ObjectCreator<FieldInjectable>(
				FieldInjectable.class);
		
		FieldInjectable injected = creator.create("dummy");
		
		Assert.assertEquals(injected.some_string, "with white spaces");
		Assert.assertEquals(injected.fDouble, 42.42D);
		Assert.assertEquals(injected.fFloat, 84.84F);
		Assert.assertEquals(injected.fIntField, 7);
		Assert.assertEquals(injected.isAwesome, true);
		Assert.assertEquals(injected.linkable, Configuration.getPid("dummy.linkable"));
	}

	@Test
	public void subclassFieldInjection() throws Exception {
		ObjectCreator<FieldInjectableSubclass> creator = new ObjectCreator<FieldInjectableSubclass>(
				FieldInjectableSubclass.class);
		
		FieldInjectableSubclass injected = creator.create("dummy2");

		Assert.assertEquals(injected.some_string, "wild wild west");
		Assert.assertEquals(injected.fDouble, 21.21D);
		Assert.assertEquals(injected.fFloat, 33.33F);
		Assert.assertEquals(injected.fIntField, 14);
		Assert.assertEquals(injected.linkable, Configuration.getPid("dummy2.linkable"));
		Assert.assertEquals(injected.isAwesome, false);
		Assert.assertEquals(injected.getString(), "yay!");
		Assert.assertEquals(injected.getDouble(), 4.23D);
	}

	@Test
	public void longestMatchingConstructor() throws Exception {
		ObjectCreator<LongMatch> creator = new ObjectCreator<LongMatch>(
				LongMatch.class);
		
		LongMatch injected = creator.create("dummy3");
		
		Assert.assertEquals(injected.fString, "wild wild west again");
		Assert.assertEquals(injected.fDouble, 99.99D);
		Assert.assertEquals(injected.fFloat, 66.66F);
		Assert.assertEquals(injected.fInt, 28);
		Assert.assertEquals(injected.fPid, Configuration.getPid("dummy3.linkable"));
	}
	
	@Test
	public void shortestMatchingConstructor() throws Exception {
		ObjectCreator<LongMatch> creator = new ObjectCreator<LongMatch>(
				LongMatch.class);
		
		LongMatch injected = creator.create("dummy4");
		
		Assert.assertEquals(injected.fString, "giuliano needs beer");
		Assert.assertEquals(injected.fDouble, 12.233D);
		
		// Rest should be default.
		Assert.assertEquals(injected.fFloat, 0.0F);
		Assert.assertEquals(injected.fInt, 0);
		Assert.assertEquals(injected.fPid, -1);
	}
	
	@Test
	public void emptyConstructor() throws Exception {
		ObjectCreator<LongMatch> creator = new ObjectCreator<LongMatch>(
				LongMatch.class);
		
		LongMatch injected = creator.create("dummy5");

		Assert.assertEquals(injected.fString, null);
		Assert.assertEquals(injected.fDouble, 0.0D);
		Assert.assertEquals(injected.fFloat, 0.0F);
		Assert.assertEquals(injected.fInt, 0);
		Assert.assertEquals(injected.fPid, -1);
	}
}

@AutoConfig
class FieldInjectable {
	@Attribute(Attribute.AUTO)
	protected String some_string;
	
	@Attribute("doubleTrouble")
	protected double fDouble;
	
	@Attribute("floating_away")
	protected float fFloat;
	
	@Attribute("very_integer")
	protected int fIntField;
	
	@Attribute(Attribute.AUTO)
	protected int linkable;
	
	@Attribute(Attribute.AUTO)
	protected boolean isAwesome;
	
	public FieldInjectable() {
		
	}
}

@AutoConfig
class FieldInjectableSubclass extends FieldInjectable {
	@Attribute(Attribute.AUTO)
	private Double double_trouble_2;
	
	@Attribute("other_string")
	private String fOtherString;
	
	public FieldInjectableSubclass() { }
	
	public String getString() {
		return fOtherString;
	}
	
	public Double getDouble() {
		return double_trouble_2;
	}
}

@AutoConfig
class LongMatch {
	
	String fString;
	double fDouble;
	float fFloat;
	int fInt;
	int fPid = -1;
	
	/** Shortest match. **/
	public LongMatch(@Attribute("some_string") String a_string, 
			@Attribute("some_double") double a_double) {
		fString = a_string;
		fDouble = a_double;
	}
	
	/** Longest match. **/
	public LongMatch(@Attribute("some_string") String a_string,
			@Attribute("some_double") double a_double,
			@Attribute("some_float") float a_float,
			@Attribute("some_int") int an_int,
			@Attribute("linkable") int a_linkable) {
		fString = a_string;
		fDouble = a_double;
		fFloat = a_float;
		fInt = an_int;
		fPid = a_linkable;
	}
	
	/** Empty match **/
	public LongMatch() {
		
	}
}

class PhantomLinkable {
	public PhantomLinkable(String s) {
		
	}
}
package it.unitn.disi.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

import junit.framework.Assert;

import org.junit.Test;

public class TestTableReader {

	@Test
	public void readHeaderedTable() throws IOException {
		StringBuffer sb = new StringBuffer();
		sb.append("name surname sex ZIP\n");
		sb.append("John Doe M 38100\n");
		sb.append("Jane Doe F 38123\n");
		sb.append("Elijah Wood ? 23020\n");
		
		TableReader tr = new TableReader(new ByteArrayInputStream(sb.toString().getBytes()));
		
		Assert.assertEquals("John", tr.get("name"));
		Assert.assertEquals("Doe", tr.get("surname"));
		Assert.assertEquals("M", tr.get("sex"));
		Assert.assertEquals("38100", tr.get("ZIP"));
		tr.next();

		Assert.assertEquals("Jane", tr.get("name"));
		Assert.assertEquals("Doe", tr.get("surname"));
		Assert.assertEquals("F", tr.get("sex"));
		Assert.assertEquals("38123", tr.get("ZIP"));
		tr.next();
		
		Assert.assertEquals("Elijah", tr.get("name"));
		Assert.assertEquals("Wood", tr.get("surname"));
		Assert.assertEquals("?", tr.get("sex"));
		Assert.assertEquals("23020", tr.get("ZIP"));
		
		try {
			tr.next();
			Assert.fail();
		} catch (NoSuchElementException ex) {
			
		}
	}
}

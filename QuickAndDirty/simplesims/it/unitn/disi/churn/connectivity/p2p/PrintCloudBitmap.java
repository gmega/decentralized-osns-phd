package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.tabular.TableReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.BitSet;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class PrintCloudBitmap implements ITransformer {

	@Attribute
	String bmp;
	
	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		@SuppressWarnings("resource")
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				new File(bmp)));

		BitSet bs = (BitSet) ois.readObject();

		TableReader reader = new TableReader(is);
		while(reader.hasNext()) {
			if (bs.get(reader.currentRow())) {
				System.out.println(reader.currentLine());
			}
			reader.next();
		}
			
	}

}

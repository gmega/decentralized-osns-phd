package it.unitn.disi.churn.connectivity.p2p;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.BitSet;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.cli.ITransformer;

@AutoConfig
public class PrintCloudBitmap implements ITransformer {

	@Attribute
	String bmp;
	
	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				new File(bmp)));

		BitSet bs = (BitSet) ois.readObject();

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		for (int i = 1; (line = reader.readLine()) != null; i++) {
			if (bs.get(i)) {
				System.out.println(line);
			}
		}
			
	}

}

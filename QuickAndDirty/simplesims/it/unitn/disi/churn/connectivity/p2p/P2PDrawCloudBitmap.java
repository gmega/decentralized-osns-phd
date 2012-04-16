package it.unitn.disi.churn.connectivity.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.churn.SequentialAttributeReader;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.collections.Pair;

@AutoConfig
public class P2PDrawCloudBitmap implements ITransformer {

	@Attribute
	double percentage;
	
	private final Random fRandom = new Random();
	
	// -------------------------------------------------------------------------

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		SequentialAttributeReader<Pair<Integer, Integer>> reader = new SequentialAttributeReader<Pair<Integer,Integer>>(is, "id") {
			
			@Override
			public Pair<Integer, Integer> read(int[] ids) throws IOException {
				int start = row();
				while(!rootChanged()) {
					advance();
				}
				// We take row() - 1 because when the root changed, we're
				// already 1 row too late. 
				return new Pair<Integer, Integer>(start, row() - 1);
			}
		};  
		
		BitSet cloud = new BitSet();
		while(reader.hasNext()) {
			Pair<Integer, Integer> interval = reader.read(null);
			int [] draw = draw(interval);
			int cut = (int) Math.ceil(percentage * draw.length); 
			for (int i = 0; i < cut; i++) {
				cloud.set(draw[i]);
			}
		}
		
		System.err.println(cloud.cardinality() + " cloud nodes.");
		
		ObjectOutputStream oStream = new ObjectOutputStream(oup);
		oStream.writeObject(cloud);
	}
	
	// -------------------------------------------------------------------------

	private int[] draw(Pair<Integer, Integer> interval) {
		int size = interval.b - interval.a + 1;
		int [] draw = new int[size];
		for (int i = 0; i < draw.length; i++) {
			draw[i] = interval.a + i;
		}
		OrderingUtils.permute(0, draw.length, draw, fRandom);
		return draw;
	}
	
	// -------------------------------------------------------------------------

}

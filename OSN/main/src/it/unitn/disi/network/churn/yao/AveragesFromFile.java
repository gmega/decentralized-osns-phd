package it.unitn.disi.network.churn.yao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.tabular.TableReader;

@AutoConfig
public class AveragesFromFile implements IAverageGenerator {
	
	private final String fFile;
	
	private TableReader fReader;

	private final boolean fWrap;

	public AveragesFromFile(@Attribute("file") String file,
			@Attribute("wrap") boolean wrap) {
		fFile = file;
		fWrap = wrap;
		reset();
	}

	private void reset() throws RuntimeException {
		try {
			if (fReader != null) {
				fReader.close();
			}
			
			fReader = new TableReader(new FileInputStream(new File(fFile)));
			fReader.next();
		} catch (IOException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	@Override
	public double nextLI() {
		return Double.parseDouble(fReader.get("li"));
	}

	@Override
	public double nextDI() {
		double di = Double.parseDouble(fReader.get("di"));
		try {
			if (!fReader.hasNext() && fWrap) {
				reset();
			} else {
				fReader.next();
			}
		} catch (IOException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
		return di;
	}

	@Override
	public String id() {
		return "file";
	}

}

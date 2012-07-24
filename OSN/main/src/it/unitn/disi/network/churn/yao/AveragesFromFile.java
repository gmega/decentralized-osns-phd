package it.unitn.disi.network.churn.yao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.tabular.TableReader;

/**
 * Pretty hacky/shaky class to allow churn parameters to be read from a file.
 * 
 * @author giuliano
 */
@AutoConfig
public class AveragesFromFile implements IAverageGenerator {

	private final String fFile;

	private TableReader fReader;

	private final boolean fWrap;

	private boolean fMarkDone;

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
			if (!fReader.hasNext()) {
				if (fWrap) {
					reset();
				}
				if (fMarkDone) {
					throw new NoSuchElementException();
				} else {
					fMarkDone = true;
				}
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

package it.unitn.disi.analysis.online;

import it.unitn.disi.network.SizeConstants;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

@AutoConfig
@StructuredLog(key = "BDW", fields = { "txbytes", "rxbytes", "max_txbytes",
		"max_rxbytes", "tx", "rx", "payload", "protocol", "overhead" })
public class NodeStatisticController implements Control {

	@Attribute("protocol")
	private int fProtocol;
	
	private TableWriter fLog;

	public NodeStatisticController(
			@Attribute("TabularLogManager") TabularLogManager manager) {
		fLog = manager.get(NodeStatisticController.class);
	}

	@Override
	public boolean execute() {
		IncrementalStats txBw = new IncrementalStats();
		IncrementalStats rxBw = new IncrementalStats();

		IncrementalStats tx = new IncrementalStats();
		IncrementalStats rx = new IncrementalStats();

		for (int i = 0; i < Network.size(); i++) {
			Node current = Network.get(i);
			NodeStatistic stat = (NodeStatistic) current.getProtocol(fProtocol);

			txBw.add(stat.transmittedBytes());
			rxBw.add(stat.receivedBytes());

			tx.add(stat.transmittedMessages());
			rx.add(stat.receivedMessages());

			stat.reset();
		}
		
		fLog.set("txbytes", txBw.getAverage());
		fLog.set("rxbytes", rxBw.getAverage()); 
		fLog.set("max_txbytes", txBw.getMax());
		fLog.set("max_rxbytes", rxBw.getMax());
		fLog.set("tx", tx.getAverage());
		fLog.set("rx", rx.getAverage());
		
		double protocol = (SizeConstants.IPV4_HEADER + SizeConstants.TCP_HEADER) * tx.getSum();
		double payload = txBw.getSum();
		
		fLog.set("payload", payload);
		fLog.set("protocol", protocol);
		fLog.set("overhead", safeDivide((protocol), (protocol + payload)));
		fLog.emmitRow();

		return false;
	}
	
	private double safeDivide(double a, double b) {
		if (b == 0.0 && a == 0.0) {
			return 0.0;
		}
		
		return a/b;
	}

}

package it.unitn.disi.sps;

import java.util.Arrays;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class DescriptorAppearanceObserver implements Control{
	
	@Attribute("linkable")
	private int fLinkable;
	
	@Attribute("dumptime")
	private long fDump;
	
	private long [] fDistribution;

	@Override
	public boolean execute() {
		long [] dVector = distributionVector();
		for (int i = 0; i < Network.size(); i++) {
			Linkable lnk = (Linkable) Network.get(i).getProtocol(fLinkable);
			for (int j = 0; j < lnk.degree(); j++) {
				Node descriptor = lnk.getNeighbor(j);
				int idx = (int) descriptor.getID();
				if (dVector[idx] == -1) {
					dVector[idx] = CommonState.getTime();
				}
			}
		}
		
		if (CommonState.getTime() == fDump) {
			System.out.println("B_" + this.getClass().getSimpleName());
			for (int i = 0; i < dVector.length; i++) {
				System.out.println(i + " " + dVector[i]);
			}
			System.out.println("E_" + this.getClass().getSimpleName());
		}
		
		return false;
	}
	
	private long [] distributionVector() {
		if (fDistribution == null){
			fDistribution = new long[Network.size()];
			Arrays.fill(fDistribution, -1);
		}
		return fDistribution;
	}
}

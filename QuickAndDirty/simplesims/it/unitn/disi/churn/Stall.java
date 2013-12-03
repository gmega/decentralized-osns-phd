package it.unitn.disi.churn;

import peersim.config.AutoConfig;

@AutoConfig
public class Stall implements Runnable{

	@Override
	public void run() {
		try {
			while(true) {
				Thread.sleep(1000);
				System.out.println("Stalling");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}

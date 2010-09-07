package peersim.core;

public class NetworkInitializer {
	public static void createNodeArray() {
		if (Network.node == null) {
			Network.node = new Node[0];
		}
	}
}

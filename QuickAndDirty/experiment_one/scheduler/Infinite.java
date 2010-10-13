package scheduler;

public class Infinite {
	public static void main(String [] args) {
		int i = 0;
		while(true) {
			i++;
			if (i % 1000000 == 0) {
				System.out.println("argh");
				System.err.println("argh1");
			}
		}
	}
}

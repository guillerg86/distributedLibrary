package Distributed;
public class LamportClock {
	int c;
	public LamportClock() {
		c = 1;
	}
	
	public int getValue() {
		return c;
	}
	
	public void tick() {
		c++;
	}
	
	public void receiveAction(int src, int sentValue) {
		c = getMAX(c, sentValue) + 1;
	}
	public void reset() {
		c = 1;
	}
	
	public static int getMAX(int value1, int value2) {
		if ( value1 > value2 ) { return value1; }
		else { return value2; }
	}
}

package io.github.oliviercailloux.minimax.utils;

public class Statistic {

	private final long tm; // time
	private final double rgrt; // regret
	private final double avgls; // loss
	private final double percQstVot; // percentage of questions asked to voters during a run

	private Statistic(long ms, double r, double l, double percentage) {
		tm = ms;
		rgrt = r;
		avgls = l;
		percQstVot = percentage;
	}

	public long getTime() {
		return tm;
	}

	public double getRegret() {
		return rgrt;
	}

	public double getAvgLoss() {
		return avgls;
	}

	public double getPercentageQstVoters() {
		return percQstVot;
	}

	public static Statistic build(long time, double regret, double avgLoss, double percentageQstVoter) {
		return new Statistic(time, regret, avgLoss, percentageQstVoter);
	}
}

package io.github.oliviercailloux.minimax.utils;

public class AggregationOperator {

	public static enum AggOps {
		MAX, AVG, WEIGHTED_AVERAGE, MIN
	}

	public static double getMax(double x, double y) {
		return x >= y ? x : y;
	}

	public static double getMin(double x, double y) {
		return x < y ? x : y;
	}

	public static double getAvg(double x, double y) {
		return (x + y) / 2;
	}

	public static double weightedAvg(double x, double y, double w1, double w2) {
		return x * w1 + y * w2;
	}

}

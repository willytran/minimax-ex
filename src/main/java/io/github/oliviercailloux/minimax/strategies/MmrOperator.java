package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.DoubleBinaryOperator;

import com.google.common.base.VerifyException;

public class MmrOperator implements DoubleBinaryOperator {

	public static MmrOperator MAX = new MmrOperator((mmr1, mmr2) -> (mmr1 >= mmr2) ? mmr1 : mmr2);

	public static MmrOperator AVERAGE = new MmrOperator((mmr1, mmr2) -> (mmr1 + mmr2) / 2);

	public static MmrOperator MIN = new MmrOperator((mmr1, mmr2) -> (mmr1 <= mmr2) ? mmr1 : mmr2);

	public static MmrOperator valueOf(String string) {
		switch (string) {
		case "MAX":
			return MmrOperator.MAX;
		case "AVERAGE":
			return MmrOperator.AVERAGE;
		case "MIN":
			return MmrOperator.MIN;
		default:
			throw new VerifyException();
		}
	}

	private DoubleBinaryOperator delegate;

	private MmrOperator(DoubleBinaryOperator delegate) {
		this.delegate = checkNotNull(delegate);
	}

	@Override
	public double applyAsDouble(double mmr1, double mmr2) {
		return delegate.applyAsDouble(mmr1, mmr2);
	}

	@Override
	public String toString() {
		if (this == MAX) {
			return "Max";
		}
		if (this == AVERAGE) {
			return "Average";
		}
		if (this == MIN) {
			return "Min";
		}
		throw new VerifyException();
	}
}

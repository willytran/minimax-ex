package io.github.oliviercailloux.minimax.strategies;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;

import com.google.common.base.MoreObjects;

public class MmrLottery {
	public static final Comparator<MmrLottery> MAX_COMPARATOR = getMaxComparator();
	
	public static final Comparator<MmrLottery> MIN_COMPARATOR = getMinComparator();

	public static MmrLottery given(double mmrIfYes, double mmrIfNo) {
		return new MmrLottery(mmrIfYes, mmrIfNo);
	}

	private static Comparator<MmrLottery> getMaxComparator() {
		final DoubleBinaryOperator max = ((mmr1, mmr2) -> (mmr1 >= mmr2) ? mmr1 : mmr2);
		final DoubleBinaryOperator min = ((mmr1, mmr2) -> (mmr1 <= mmr2) ? mmr1 : mmr2);

		final Comparator<MmrLottery> comparingMaxes = Comparator
				.comparing(l -> max.applyAsDouble(l.getMmrIfYes(), l.getMmrIfNo()));
		final Comparator<MmrLottery> comparingLexicographically = comparingMaxes
				.thenComparing(l -> min.applyAsDouble(l.getMmrIfYes(), l.getMmrIfNo()));
		return comparingLexicographically;
	}

	private static Comparator<MmrLottery> getMinComparator() {
		final DoubleBinaryOperator max = ((mmr1, mmr2) -> (mmr1 >= mmr2) ? mmr1 : mmr2);
		final DoubleBinaryOperator min = ((mmr1, mmr2) -> (mmr1 <= mmr2) ? mmr1 : mmr2);

		final Comparator<MmrLottery> comparingMins = Comparator
				.comparing(l -> min.applyAsDouble(l.getMmrIfYes(), l.getMmrIfNo()));
		final Comparator<MmrLottery> comparingLexicographically = comparingMins
				.thenComparing(l -> max.applyAsDouble(l.getMmrIfYes(), l.getMmrIfNo()));
		return comparingLexicographically;
	}
	
	private double mmrIfYes;
	private double mmrIfNo;

	private MmrLottery(double mmrIfYes, double mmrIfNo) {
		this.mmrIfYes = mmrIfYes;
		this.mmrIfNo = mmrIfNo;
	}

	public double getMmrIfYes() {
		return mmrIfYes;
	}

	public double getMmrIfNo() {
		return mmrIfNo;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mmrIfYes, mmrIfNo);
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof MmrLottery)) {
			return false;
		}

		final MmrLottery q2 = (MmrLottery) o2;
		return mmrIfYes == q2.mmrIfYes && mmrIfNo == q2.mmrIfNo;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("mmrIfYes", mmrIfYes).add("mmrIfNo", mmrIfNo).toString();
	}
}
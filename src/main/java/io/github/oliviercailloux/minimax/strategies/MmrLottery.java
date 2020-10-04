package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Objects;

import com.google.common.base.MoreObjects;

public class MmrLottery {

	private static class ComparatorWithDescription<T> implements Comparator<T> {
		private final Comparator<T> delegate;
		private final String description;

		private ComparatorWithDescription(Comparator<T> delegate, String description) {
			this.delegate = checkNotNull(delegate);
			this.description = checkNotNull(description);
		}

		@Override
		public int compare(T l1, T l2) {
			return delegate.compare(l1, l2);
		}

		@Override
		public String toString() {
			return description;
		}
	}

	/**
	 * MAX_COMPARATOR is pessimistic. It considers a question Q1 greater than a
	 * question Q2 if (maxQ1>maxQ2) or (maxQ1=maxQ2 & minQ1>minQ2)
	 *
	 * Thus, the “smallest” question is the best one as we seek to minimize regret.
	 *
	 */
	public static final Comparator<MmrLottery> MAX_COMPARATOR = getMaxComparator();

	public static final Comparator<MmrLottery> MIN_COMPARATOR = getMinComparator();

	public static MmrLottery given(double mmrIfYes, double mmrIfNo) {
		return new MmrLottery(mmrIfYes, mmrIfNo);
	}

	private static Comparator<MmrLottery> getMaxComparator() {
		/**
		 * Comparing using pure lexicographic reasoning results in (1.0, 2.0) being
		 * considered lower than (0.0, 2.0+1e-16), which we do not want (I have seen a
		 * similar situation with max MMRs differing only at the 17th decimal).
		 */
		return new ComparatorWithDescription<>(
				Comparator.comparingDouble(l -> l.getWorstMmr() + (l.getBestMmr() / 1e6d)), "MAX");
	}

	private static Comparator<MmrLottery> getMinComparator() {
		return new ComparatorWithDescription<>(
				Comparator.comparingDouble(l -> l.getBestMmr() + (l.getWorstMmr() / 1e6d)), "MIN");
	}

	private final double mmrIfYes;
	private final double mmrIfNo;

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

	/**
	 * @return a maximal MMR among the two.
	 */
	public double getWorstMmr() {
		return mmrIfYes >= mmrIfNo ? mmrIfYes : mmrIfNo;
	}

	/**
	 * @return a minimal MMR among the two.
	 */
	public double getBestMmr() {
		return mmrIfYes <= mmrIfNo ? mmrIfYes : mmrIfNo;
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
package io.github.oliviercailloux.minimax.strategies;

import java.util.Objects;

import com.google.common.base.MoreObjects;

public class MmrLottery {
	public static MmrLottery given(double mmrIfYes, double mmrIfNo) {
		return new MmrLottery(mmrIfYes, mmrIfNo);
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
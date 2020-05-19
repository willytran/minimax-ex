package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Objects;

import org.apfloat.Aprational;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;

/**
 *
 * Immutable. May represent an empty vector.
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
public class PSRWeights {

	private final ImmutableList<Double> weights;
	private final double upperBound = 1d;
	private final double lowerBound = 0d;

	public static PSRWeights given(List<Double> weights) {
		return new PSRWeights(weights);
	}

	private PSRWeights(List<Double> weights) {
		Preconditions.checkNotNull(weights);
		checkArgument(weights.isEmpty() || weights.get(0) == upperBound);
		checkArgument(weights.size() <= 1 || weights.get(weights.size() - 1) == lowerBound);
		this.weights = ImmutableList.copyOf(weights);
		checkArgument(isConvex(), weights);
	}

	private boolean isConvex() {
		double wi1, wi2, wi3;
		for (int i = 0; i < weights.size() - 2; i++) {
			wi1 = weights.get(i);
			wi2 = weights.get(i + 1);
			wi3 = weights.get(i + 2);
			if ((wi1 - wi2) < (wi2 - wi3)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Retrieves the weight associated to the given rank. The first position of the
	 * ranking is 1.
	 *
	 * @return the weight of the given rank
	 */
	public double getWeightAtRank(int rank) {
		return this.weights.get(rank - 1);
	}

	public ImmutableList<Double> getWeights() {
		return this.weights;
	}

	/**
	 * Returns the number of ranks.
	 *
	 * @return ≥ 0.
	 */
	public int size() {
		return weights.size();
	}

	/**
	 * Given a query d * (w_i − w_{i+1}) >= n * (w_{i+1} − w_{i+2}) where n/d = λ
	 *
	 * @return the appropriate information indicating whether the term on the left
	 *         is greater than, equal to or lower than the right one
	 */
	public CommitteePreferenceInformation askQuestion(QuestionCommittee qc) {
		int i = qc.getRank();
		Aprational lambda = qc.getLambda();
		double left = lambda.denominator().intValue() * (weights.get(i - 1) - weights.get(i));
		double right = lambda.numerator().intValue() * (weights.get(i) - weights.get(i + 1));
		final ComparisonOperator op;
		if (left > right) {
			op = ComparisonOperator.GE;
		} else if (left == right) {
			op = ComparisonOperator.EQ;
		} else {
			op = ComparisonOperator.LE;
		}
		return CommitteePreferenceInformation.given(i, op, lambda);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).addValue(weights).toString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PSRWeights)) {
			return false;
		}
		PSRWeights w = (PSRWeights) o;
		return w.weights.equals(this.weights);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.weights);
	}

}
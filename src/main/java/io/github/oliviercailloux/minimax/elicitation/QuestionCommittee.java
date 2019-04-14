package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

import org.apfloat.Aprational;

import com.google.common.base.MoreObjects;

/**
 * Immutable. A Question to the (chair of the) committee, with the form: is (w_r
 * − w_{r+1}) ≥ λ (w_{r+1} − w_{r+2})? With the convention that <em>r</em>
 * equals one for the first rank.
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
public class QuestionCommittee {

	public static QuestionCommittee given(Aprational lambda, int rank) {
		return new QuestionCommittee(lambda, rank);
	}

	private final Aprational lambda;
	private final int rank;

	private QuestionCommittee(Aprational lambda, int rank) {
		this.lambda = requireNonNull(lambda);
		checkArgument(rank >= 1);
		this.rank = rank;
	}

	public Aprational getLambda() {
		return lambda;
	}

	/**
	 * Returns <em>r</em>.
	 *
	 * @return ≥ 1.
	 */
	public int getRank() {
		return rank;
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof QuestionCommittee)) {
			return false;
		}
		final QuestionCommittee q2 = (QuestionCommittee) o2;
		return Objects.equals(lambda, q2.lambda) && Objects.equals(rank, q2.rank);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lambda, rank);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("λ", lambda).add("rank", rank).toString();
	}

}

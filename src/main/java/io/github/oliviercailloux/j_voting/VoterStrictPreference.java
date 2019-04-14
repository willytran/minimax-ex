package io.github.oliviercailloux.j_voting;

import static java.util.Objects.requireNonNull;

import java.util.List;

import com.google.common.base.MoreObjects;

import io.github.oliviercailloux.minimax.elicitation.Answer;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.StrictPreference;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/**
 * Immutable.
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
public class VoterStrictPreference {

	/**
	 * Returns a voterStrictPreference object.
	 *
	 * @param voter              the person that has these preferences.
	 * @param rankedAlternatives with the preferred alternative as first element
	 *                           (may be empty).
	 * @return a voter strict preference object.
	 */
	public static VoterStrictPreference given(Voter voter, List<Alternative> rankedAlternatives) {
		return new VoterStrictPreference(voter, rankedAlternatives);
	}

	private final Voter voter;
	private final StrictPreference preference;

	private VoterStrictPreference(Voter voter, List<Alternative> rankedAlternatives) {
		this.voter = requireNonNull(voter);
		this.preference = new StrictPreference(rankedAlternatives);
	}

	/**
	 * Returns the alternatives this preference is about, the most preferred one
	 * first.
	 *
	 * @return a list of {@link #size()} elements.
	 */
	public List<Alternative> getAlternatives() {
		return preference.getAlternatives();
	}

	/**
	 * Returns the number of alternatives, or equivalently, of ranks, this
	 * preference is about.
	 *
	 * @return ≥ 0.
	 */
	public int size() {
		return preference.size();
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof VoterStrictPreference)) {
			return false;
		}
		final VoterStrictPreference v2 = (VoterStrictPreference) o2;
		return v2.preference.equals(preference);
	}

	@Override
	public int hashCode() {
		return preference.hashCode();
	}

	/**
	 * Returns the rank of the given alternative.
	 *
	 * @param alternative not <code>null</code>.
	 * @return ≥ 1.
	 * @throws IllegalArgumentException if the given alternative is not in this
	 *                                  preference.
	 */
	public int getAlternativeRank(Alternative alternative) {
		return preference.getAlternativeRank(requireNonNull(alternative));
	}

	/**
	 * Returns the alternative at the given rank.
	 *
	 * @param rank ≥ 1.
	 * @return an alternative.
	 */
	public Alternative getAlternativeAtRank(int rank) {
		return preference.getAlternatives().get(rank - 1);
	}

	public Voter getVoter() {
		return voter;
	}

	public StrictPreference asStrictPreference() {
		return preference;
	}

	/**
	 * Given a query a > b
	 *
	 * @return if a is greater or lower than b
	 */
	public Answer askQuestion(QuestionVoter qv) {
		if (preference.getAlternativeRank(qv.getFirstAlternative()) < preference
				.getAlternativeRank(qv.getSecondAlternative())) {
			return Answer.GREATER;
		}
		return Answer.LOWER;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("voter", voter).add("preference", preference).toString();
	}
}

package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.google.common.base.MoreObjects;

import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/**
 * Immutable.
 */
public class VoterPreferenceInformation {

	public static VoterPreferenceInformation given(Voter voter, Alternative best, Alternative worst) {
		return new VoterPreferenceInformation(voter, best, worst);
	}

	private final Voter voter;
	private final Alternative best, worst;

	private VoterPreferenceInformation(Voter voter, Alternative best, Alternative worst) {
		checkArgument(!best.equals(worst));
		this.voter = checkNotNull(voter);
		this.best = checkNotNull(best);
		this.worst = checkNotNull(worst);
	}

	public Voter getVoter() {
		return this.voter;
	}

	public Alternative getBestAlternative() {
		return best;
	}

	public Alternative getWorstAlternative() {
		return worst;
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof VoterPreferenceInformation)) {
			return false;
		}
		final VoterPreferenceInformation i2 = (VoterPreferenceInformation) o2;
		return Objects.equals(voter, i2.voter) && Objects.equals(best, i2.best) && Objects.equals(worst, i2.worst);
	}

	@Override
	public int hashCode() {
		return Objects.hash(voter, best, worst);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("voter", voter).add("best", best).add("worst", worst).toString();
	}

}

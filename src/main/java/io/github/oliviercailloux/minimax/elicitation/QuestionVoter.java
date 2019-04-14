package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.google.common.base.MoreObjects;

import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/**
 * Immutable. A Question to a voter with the form: is alternative <em>a</em>
 * weakly preferred to <em>b</em>? The alternatives <em>a</em> and <em>b</em>
 * are different.
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
public class QuestionVoter {

	public static QuestionVoter given(Voter voter, Alternative a, Alternative b) {
		return new QuestionVoter(voter, a, b);
	}

	private final Voter voter;
	private final Alternative a, b;

	private QuestionVoter(Voter voter, Alternative a, Alternative b) {
		checkArgument(!a.equals(b));
		this.voter = requireNonNull(voter);
		this.a = requireNonNull(a);
		this.b = requireNonNull(b);
	}

	public Voter getVoter() {
		return this.voter;
	}

	public Alternative getFirstAlternative() {
		return a;
	}

	public Alternative getSecondAlternative() {
		return b;
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof QuestionVoter)) {
			return false;
		}
		final QuestionVoter q2 = (QuestionVoter) o2;
		return Objects.equals(voter, q2.voter) && Objects.equals(a, q2.a) && Objects.equals(b, q2.b);
	}

	@Override
	public int hashCode() {
		return Objects.hash(voter, a, b);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("voter", voter).add("a", a).add("b", b).toString();
	}

}

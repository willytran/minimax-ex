package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Objects;

import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/**
 * Immutable. A Question to a voter with the form: is alternative <em>a</em>
 * preferred to <em>b</em>? The alternatives <em>a</em> and <em>b</em> are
 * different.
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
@JsonbPropertyOrder({ "voter", "alternatives" })
public class QuestionVoter {

	public static QuestionVoter given(Voter voter, Alternative a, Alternative b) {
		return new QuestionVoter(voter, a, b);
	}

	private final Voter voter;
	private final Alternative a, b;

	private QuestionVoter(Voter voter, Alternative a, Alternative b) {
		checkArgument(!a.equals(b));
		this.voter = checkNotNull(voter);
		this.a = checkNotNull(a);
		this.b = checkNotNull(b);
	}

	public Voter getVoter() {
		return this.voter;
	}

	public ImmutableSet<Alternative> getAlternatives() {
		return ImmutableSortedSet.orderedBy(Comparator.comparing(Alternative::getId)).add(a, b).build();
	}

	@JsonbTransient
	public Alternative getFirstAlternative() {
		return a;
	}

	@JsonbTransient
	public Alternative getSecondAlternative() {
		return b;
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof QuestionVoter)) {
			return false;
		}
		final QuestionVoter q2 = (QuestionVoter) o2;
		return Objects.equals(voter, q2.voter) && Objects.equals(getAlternatives(), q2.getAlternatives());
	}

	@Override
	public int hashCode() {
		return Objects.hash(voter, getAlternatives());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("voter", voter).add("alternatives", getAlternatives()).toString();
	}

}

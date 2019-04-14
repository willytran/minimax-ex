package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/**
 * Immutable. Contains at least one voter (otherwise it would be impossible to
 * provide a list of alternatives).
 *
 * @author xoxor
 * @author Olivier Cailloux
 *
 */
public class Oracle {

	public static Oracle build(ImmutableMap<Voter, VoterStrictPreference> profile, PSRWeights weights) {
		return new Oracle(profile, weights);
	}

	private final ImmutableMap<Voter, VoterStrictPreference> profile;
	private final PSRWeights weights;
	private final ImmutableSortedSet<Alternative> alternatives;

	private Oracle(Map<Voter, VoterStrictPreference> profile, PSRWeights weights) {
		checkArgument(profile.size() >= 1);
		this.profile = ImmutableMap.copyOf(profile);
		this.weights = requireNonNull(weights);
		checkArgument(profile.entrySet().stream().allMatch((e) -> e.getValue().getVoter().equals(e.getKey())));

		final Comparator<Alternative> comparingIds = Comparator.comparingInt(Alternative::getId);
		final List<Alternative> alternativesList = profile.values().stream().findAny().get().getAlternatives();
		this.alternatives = ImmutableSortedSet.copyOf(comparingIds, alternativesList);

		checkArgument(profile.values().stream().map(VoterStrictPreference::getAlternatives)
				.allMatch((l) -> ImmutableSet.copyOf(l).equals(alternatives)));
		final int nbAlts = alternatives.size();
		checkArgument(weights.size() == nbAlts);
	}

	public Answer getAnswer(Question q) {
		switch (q.getType()) {
		case VOTER_QUESTION: {
			QuestionVoter qv = q.getQuestionVoter();
			Voter v = qv.getVoter();
			checkArgument(profile.containsKey(v));
			VoterStrictPreference vsp = profile.get(v);
			return vsp.askQuestion(qv);
		}
		case COMMITTEE_QUESTION: {
			QuestionCommittee qc = q.getQuestionCommittee();
			return weights.askQuestion(qc);
		}
		default:
			throw new AssertionError();
		}
	}

	public ImmutableMap<Voter, VoterStrictPreference> getProfile() {
		return profile;
	}

	/**
	 * Returns the alternatives this profile is about. The size of the weights
	 * vector is also the size of the returned set.
	 *
	 * @return the alternatives all voters’ preferences are about.
	 */
	public ImmutableSet<Alternative> getAlternatives() {
		return alternatives;
	}

	public PSRWeights getWeights() {
		return weights;
	}

	public int getM() {
		return alternatives.size();
	}

	public int getN() {
		return profile.size();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("profile", profile).add("weights", weights).toString();
	}
}

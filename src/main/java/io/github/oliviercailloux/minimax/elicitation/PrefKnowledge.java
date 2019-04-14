package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apfloat.Apint;
import org.apfloat.Aprational;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.utils.Rounder;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class PrefKnowledge {
	public static PrefKnowledge given(Set<Alternative> alternatives, Set<Voter> voters) {
		return new PrefKnowledge(alternatives, voters);
	}

	/**
	 * TODO May be incorrect. Consider removing?
	 */
	public static PrefKnowledge copyOf(PrefKnowledge knowledge) {
		PrefKnowledge pref = new PrefKnowledge(knowledge.getAlternatives(), knowledge.getVoters());
		Map<Voter, VoterPartialPreference> profile = new HashMap<>();
		for (Voter v : knowledge.getVoters()) {
			VoterPartialPreference vp = VoterPartialPreference.copyOf(knowledge.getProfile().get(v));
			profile.put(v, vp);
		}
		pref.partialProfile = ImmutableMap.copyOf(profile);
		pref.cow = ConstraintsOnWeights.copyOf(knowledge.getConstraintsOnWeights());
		return pref;
	}

	private ImmutableSet<Alternative> alternatives;
	private ImmutableMap<Voter, VoterPartialPreference> partialProfile;
	private ConstraintsOnWeights cow;
	private Map<Integer, Range<Aprational>> lambdaRanges;

	private PrefKnowledge(Set<Alternative> alternatives, Set<Voter> voters) {
		this.alternatives = ImmutableSet.copyOf(alternatives);

		final int m = alternatives.size();
		final int n = voters.size();

		checkArgument(m >= 1);
		checkArgument(n >= 1);

		cow = ConstraintsOnWeights.withRankNumber(m);
		cow.setConvexityConstraint();

		final ImmutableMap.Builder<Voter, VoterPartialPreference> builder = ImmutableMap.builder();
		for (Voter voter : voters) {
			builder.put(voter, VoterPartialPreference.about(voter, alternatives));
		}
		partialProfile = builder.build();

		if (m == 1) {
			lambdaRanges = null;
		} else {
			lambdaRanges = new LinkedHashMap<>(m - 2);
			for (int rank = 1; rank <= m - 2; ++rank) {
				assert m - 2 >= 1;
				/**
				 * When d_i/d_{i+1} > n−1, the rule is plurality-i-PD, meaning that when two
				 * alternatives share the same number of times they reach rank j, for all j < i,
				 * then what counts is the number of times the alternative reaches the rank i,
				 * and if ex-æquo, they are resolved using the further weight constraints for
				 * lower ranks. Thus, it is unnecessary to distinguish weights greater than n−1.
				 */
				lambdaRanges.put(rank, Range.closed(new Apint(1), new Apint(n)));
			}
		}
	}
	
	public void setRounder(Rounder r) {
		cow.setRounder(r);
	}

	/**
	 * @return a non empty set.
	 */
	public ImmutableSet<Alternative> getAlternatives() {
		return alternatives;
	}

	/**
	 * @return a non-empty set.
	 */
	public ImmutableSet<Voter> getVoters() {
		return partialProfile.keySet();
	}

	/**
	 * Adds the constraint: (w_i − w_{i+1}) OP λ (w_{i+1} − w_{i+2}).
	 *
	 * @param rank   1 ≤ rank ≤ m-2.
	 * @param op     the operator.
	 * @param lambda a finite value.
	 */
	public void addConstraint(int rank, ComparisonOperator op, Aprational lambda) {
		checkArgument(rank >= 1);
		checkArgument(rank <= alternatives.size() - 2);
		cow.addConstraint(rank, op, lambda.doubleValue());

		/** The constraint is that D_i/D_{i+1} OP lambda. */
		final Range<Aprational> providedRange;
		switch (op) {
		case EQ:
			providedRange = Range.closed(lambda, lambda);
			break;
		case GE:
			providedRange = Range.atLeast(lambda);
			break;
		case LE:
			providedRange = Range.atMost(lambda);
			break;
		default:
			throw new AssertionError();
		}
		final Range<Aprational> existingRange = lambdaRanges.get(rank);
		checkArgument(existingRange.isConnected(providedRange),
				"The provided constraint makes the program infeasible.");
		final Range<Aprational> restr = existingRange.intersection(providedRange);
		checkArgument(!restr.isEmpty(), "The provided constraint makes the program (just) infeasible.");
		lambdaRanges.put(rank, restr);
	}

	public ImmutableMap<Voter, VoterPartialPreference> getProfile() {
		return partialProfile;
	}

	public VoterPartialPreference getPartialPreference(Voter voter) {
		return partialProfile.get(voter);
	}

	public ConstraintsOnWeights getConstraintsOnWeights() {
		return cow;
	}

	public Range<Aprational> getLambdaRange(int rank) {
		checkArgument(rank >= 1);
		checkArgument(rank <= alternatives.size() - 2);
		return lambdaRanges.get(rank);
	}

}

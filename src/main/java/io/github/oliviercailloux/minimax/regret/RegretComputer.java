package io.github.oliviercailloux.minimax.regret;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.graph.ImmutableGraph;

import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.jlp.elements.SumTerms;
import io.github.oliviercailloux.jlp.elements.SumTermsBuilder;
import io.github.oliviercailloux.jlp.elements.Term;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class RegretComputer {
	private final PrefKnowledge knowledge;

	public RegretComputer(PrefKnowledge knowledge) {
		this.knowledge = requireNonNull(knowledge);
	}

	public Regrets getMinimalMaxRegrets() {
		return getAllPairwiseMaxRegrets().getMinimalMaxRegrets();
	}

	ImmutableSet<PairwiseMaxRegret> getPairwiseMaxRegrets(Alternative x) {
		checkArgument(knowledge.getAlternatives().contains(x));

		final ImmutableMap<Voter, Integer> ranksOfX = getWorstRanksOfX(x);
		final ImmutableSortedMultiset<Integer> multiSetOfRanksOfX = ImmutableSortedMultiset.copyOf(ranksOfX.values());

		final ImmutableSet<PairwiseMaxRegret> pmrs = knowledge.getAlternatives().stream()
				.map((y) -> getPmr(x, y, ranksOfX, multiSetOfRanksOfX)).collect(ImmutableSet.toImmutableSet());
		verify(!pmrs.isEmpty());

		return pmrs;
	}

	public Regrets getAllPairwiseMaxRegrets() {
		final ImmutableMap<Alternative, ImmutableSet<PairwiseMaxRegret>> allPmrs = knowledge.getAlternatives().stream()
				.collect(ImmutableMap.toImmutableMap(Function.identity(), this::getPairwiseMaxRegrets));
		return Regrets.given(allPmrs);
	}

	private PairwiseMaxRegret getPmr(Alternative x, Alternative y, Map<Voter, Integer> ranksOfX,
			SortedMultiset<Integer> multiSetOfRanksOfX) {
		final int m = knowledge.getAlternatives().size();
		final ImmutableMap<Voter, Integer> ranksOfY = getBestRanksOfY(x, y);
		final ImmutableSortedMultiset<Integer> multiSetOfRanksOfY = ImmutableSortedMultiset.copyOf(ranksOfY.values());

		final SumTermsBuilder builder = SumTerms.builder();
		for (int r = 1; r <= m; ++r) {
			final int coefY = multiSetOfRanksOfY.count(r);
			final int coefX = multiSetOfRanksOfX.count(r);
			final int coef = coefY - coefX;
			if (coef != 0) {
				final Term term = knowledge.getConstraintsOnWeights().getTerm(coef, r);
				builder.add(term);
			}
		}
		final double pmr = knowledge.getConstraintsOnWeights().maximize(builder.build());
//		assert (Math.pow(10, -1 * EPSILON_EXPONENT) > ConstraintsOnWeights.EPSILON);
		PairwiseMaxRegret pmrY;
		if (x.equals(y)) {
			pmrY = PairwiseMaxRegret.given(x, y, ranksOfX, ranksOfX,
					knowledge.getConstraintsOnWeights().getLastSolution(), 0);
		} else {
			pmrY = PairwiseMaxRegret.given(x, y, ranksOfX, ranksOfY,
					knowledge.getConstraintsOnWeights().getLastSolution(), pmr);
		}
		return pmrY;
	}

	public ImmutableMap<Voter, Integer> getWorstRanksOfX(Alternative x) {
		final ImmutableMap<Voter, Integer> ranksOfX;
		final ImmutableMap.Builder<Voter, Integer> ranksOfXBuilder = ImmutableMap.builder();
		for (Voter voter : knowledge.getVoters()) {
			final int rankX = getWorstRankOfX(x, knowledge.getPartialPreference(voter));
			ranksOfXBuilder.put(voter, rankX);
		}
		ranksOfX = ranksOfXBuilder.build();
		return ranksOfX;
	}

	int getWorstRankOfX(Alternative x, VoterPartialPreference partialPreference) {
		final ImmutableGraph<Alternative> transitivePreference = partialPreference.asTransitiveGraph();
		final int m = transitivePreference.nodes().size();
		/** +1 because x itself is to be counted. */
		final int nbWeaklyLessGoodThanX = transitivePreference.successors(x).size() + 1;
		assert 1 <= nbWeaklyLessGoodThanX && nbWeaklyLessGoodThanX <= m;
		final int nbNotWeaklyLessGoodThanX = m - nbWeaklyLessGoodThanX;
		final int rankX = 1 + nbNotWeaklyLessGoodThanX;
		assert 1 <= rankX && rankX <= m;
		return rankX;
	}

	public ImmutableMap<Voter, Integer> getBestRanksOfY(Alternative x, Alternative y) {
		final ImmutableMap<Voter, Integer> ranksOfY;
		final ImmutableMap.Builder<Voter, Integer> ranksOfYBuilder = ImmutableMap.builder();
		for (Voter voter : knowledge.getVoters()) {
			final int rankY = getBestRankOfY(x, y, knowledge.getPartialPreference(voter));
			ranksOfYBuilder.put(voter, rankY);
		}
		ranksOfY = ranksOfYBuilder.build();
		return ranksOfY;
	}

	int getBestRankOfY(Alternative x, Alternative y, VoterPartialPreference partialPreference) {
		final ImmutableGraph<Alternative> transitivePreference = partialPreference.asTransitiveGraph();
		final int m = transitivePreference.nodes().size();
		Set<Alternative> strictlyBetterThanY = transitivePreference.predecessors(y);
		final int nbStrictlyBetterThanY = strictlyBetterThanY.size();
		assert 0 <= nbStrictlyBetterThanY && nbStrictlyBetterThanY <= m - 1;
		final int beta;
		if (transitivePreference.hasEdgeConnecting(x, y) || x.equals(y)) {
			HashSet<Alternative> incomparableAlternatives = new HashSet<>(transitivePreference.nodes());
			HashSet<Alternative> notBetterThanX = new HashSet<>(transitivePreference.successors(x));
			notBetterThanX.add(x);
			incomparableAlternatives.removeAll(notBetterThanX);
			incomparableAlternatives.removeAll(strictlyBetterThanY);

			final int nbIncomparableAlts = incomparableAlternatives.size();
			assert 0 <= nbIncomparableAlts && nbIncomparableAlts <= m - 1;
			beta = nbIncomparableAlts;
		} else {
			beta = 0;
		}
		final int rankY = 1 + nbStrictlyBetterThanY + beta;
		assert 1 <= rankY && rankY <= m;
		return rankY;
	}
}

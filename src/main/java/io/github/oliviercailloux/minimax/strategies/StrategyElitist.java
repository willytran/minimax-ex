package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkState;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.graph.ImmutableGraph;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.minimax.regret.PairwiseMaxRegret;

public class StrategyElitist implements Strategy {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyElitist.class);

	public static StrategyElitist newInstance() {
		return new StrategyElitist();
	}

	private StrategyHelper helper;

	private StrategyElitist() {
		helper = StrategyHelper.newInstance();
	}

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		helper.setKnowledge(knowledge);
	}

	@Override
	public Question nextQuestion() {
		checkState(helper != null);
		final int m = helper.getKnowledge().getAlternatives().size();

		final ImmutableCollection<VoterPartialPreference> prefs = helper.getKnowledge().getProfile().values();
		for (VoterPartialPreference pref : prefs) {
			final ImmutableGraph<Alternative> graph = pref.asTransitiveGraph();
			final Set<Alternative> alternatives = graph.nodes();
			final Alternative topAlternative = alternatives.stream()
					.max(Comparator.comparing(a -> graph.successors(a).size())).get();

			if (graph.successors(topAlternative).size() < m - 1) {
				final Alternative incomparable = StrategyHelper.getIncomparables(graph, topAlternative)
						.sorted(Comparator.naturalOrder()).findFirst().get();
				return Question.toVoter(QuestionVoter.given(pref.getVoter(), topAlternative, incomparable));
			}
		}

		/** To fix: this repeats the strategy by mmr. */
		final ImmutableSetMultimap<Alternative, PairwiseMaxRegret> mmrs = helper.getMinimalMaxRegrets().asMultimap();

		final Alternative xStar = helper.drawFromStrictlyIncreasing(mmrs.keySet().asList(), Comparator.naturalOrder());
		final ImmutableSet<PairwiseMaxRegret> pmrs = mmrs.get(xStar).stream().collect(ImmutableSet.toImmutableSet());
		final PairwiseMaxRegret pmr = helper.drawFromStrictlyIncreasing(pmrs.asList(),
				PairwiseMaxRegret.BY_ALTERNATIVES);

		final PSRWeights wBar = pmr.getWeights();
		final PSRWeights wMin = helper.getMinTauW(pmr);
		final ImmutableMap<Integer, Double> valuedRanks = IntStream.rangeClosed(1, m - 2).boxed()
				.collect(ImmutableMap.toImmutableMap(i -> i, i -> getSpread(wBar, wMin, i)));
		final ImmutableSet<Integer> minSpreadRanks = StrategyHelper.getMinimalElements(valuedRanks);
		final QuestionCommittee qC = helper.getQuestionAboutHalfRange(
				helper.drawFromStrictlyIncreasing(minSpreadRanks.asList(), Comparator.naturalOrder()));
		LOGGER.info("Questioning committee: {}.", qC);
		return Question.toCommittee(qC);
	}

	private double getSpread(PSRWeights wBar, PSRWeights wMin, int i) {
		return IntStream.rangeClosed(0, 2).boxed()
				.mapToDouble(k -> Math.abs(wBar.getWeightAtRank(i + k) - wMin.getWeightAtRank(i + k))).sum();
	}
}

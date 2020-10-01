package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.Comparator;
import java.util.Set;

import com.google.common.collect.ImmutableCollection;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableGraph;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;

public class StrategyElitist implements Strategy {
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

		final Comparator<EndpointPair<Alternative>> comparingPair = Comparator.comparing(EndpointPair::nodeU);
		final Comparator<EndpointPair<Alternative>> c2 = comparingPair.thenComparing(EndpointPair::nodeV);
		for (VoterPartialPreference pref : prefs) {
			final ImmutableGraph<Alternative> graph = pref.asTransitiveGraph();
			if (graph.edges().size() < m * (m - 1) / 2) {
				final EndpointPair<Alternative> pair = helper.sortAndDraw(StrategyHelper.getIncomparablePairs(graph),
						c2);
				return Question.toVoter(pref.getVoter(), pair.nodeU(), pair.nodeV());
			}
			verify(graph.edges().size() == m * (m - 1) / 2);
		}

		verify(helper.getKnowledge().isProfileComplete());
		return Question.toCommittee(helper.getQuestionAboutHalfRange(1));
	}
}

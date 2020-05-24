package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Verify.verify;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.graph.ImmutableGraph;

import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyRandom implements Strategy {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyRandom.class);

	public static StrategyRandom build() {
		return new StrategyRandom();
	}

	private final StrategyHelper helper;

	private StrategyRandom() {
		helper = StrategyHelper.newInstance();
	}

	public void setRandom(Random random) {
		helper.setRandom(random);
	}

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		helper.setKnowledge(knowledge);
	}

	@Override
	public Question nextQuestion() {
		final int m = helper.getAndCheckM();

		final ImmutableSet<Voter> questionableVoters = helper.getQuestionableVoters();
		final boolean askVoters;
		if (questionableVoters.isEmpty()) {
			verify(m > 2);
			askVoters = false;
		} else {
			if (m == 2) {
				askVoters = true;
			} else {
				askVoters = helper.getRandom().nextBoolean();
			}
		}

		final Question question;
		if (askVoters) {
			final Voter voter = helper.draw(questionableVoters);
			final ImmutableGraph<Alternative> graph = helper.getKnowledge().getPartialPreference(voter)
					.asTransitiveGraph();
			final ImmutableSetMultimap<Alternative, Alternative> incomparables = graph.nodes().stream()
					.collect(ImmutableSetMultimap.flatteningToImmutableSetMultimap(a -> a,
							a -> StrategyHelper.getIncomparables(graph, a)));
			final Alternative a = helper.draw(incomparables.keySet());
			final Alternative b = helper.draw(incomparables.get(a));
			question = Question.toVoter(voter, a, b);
		} else {
			verify(m >= 3);
			final int rank = helper.nextInt(m - 2) + 1;
			final QuestionCommittee qc = helper.getQuestionAboutHalfRange(rank);
			question = Question.toCommittee(qc);
		}

		return question;
	}
}

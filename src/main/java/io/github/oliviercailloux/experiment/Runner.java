package io.github.oliviercailloux.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.minimax.Strategy;
import io.github.oliviercailloux.minimax.StrategyTwoPhasesHeuristic;
import io.github.oliviercailloux.minimax.elicitation.Answer;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.regret.Regrets;
import io.github.oliviercailloux.minimax.utils.Generator;

public class Runner {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

	public static void main(String[] args) {
		final boolean committeeFirst = true;
		final int k = 200;
		final Strategy strategy = StrategyTwoPhasesHeuristic.build(k, 0, committeeFirst);
		final Run run = run(strategy, 10, 20, k);
		final ImmutableList<Regrets> regrets = run.getMinimalMaxRegrets();
		LOGGER.info("All regret values: {}.",
				regrets.stream().map(Regrets::getMinimalMaxRegretValue).collect(ImmutableList.toImmutableList()));
	}

	public static Run run(Strategy strategy, int m, int n, int k) {
		final Oracle oracle = Oracle.build(ImmutableMap.copyOf(Generator.genProfile(m, n)), Generator.genWeights(m));

		final PrefKnowledge knowledge = PrefKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
		strategy.setKnowledge(knowledge);

		final ImmutableList.Builder<Question> qBuilder = ImmutableList.builder();
		final ImmutableList.Builder<Long> tBuilder = ImmutableList.builder();
		for (int i = 1; i <= k; i++) {
			final long startTime = System.currentTimeMillis();
			final Question q = strategy.nextQuestion();
			LOGGER.debug("Asked {}.", q);
			final Answer a = oracle.getAnswer(q);
			knowledge.update(q, a);
			tBuilder.add(startTime);
			qBuilder.add(q);
		}

		final long endTime = System.currentTimeMillis();

		return Run.of(oracle, tBuilder.build(), qBuilder.build(), endTime);
	}
}

package io.github.oliviercailloux.minimax.experiment;

import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.minimax.strategies.MmrOperator;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class PessimisticXp {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(PessimisticXp.class);

	public static void main(String[] args) {
		final StrategyFactory pessimisticFactory = StrategyFactory.aggregatingMmrs(MmrOperator.MAX);
		/** (4, 4, 100): 21 sec; need 19 q to reach 0. */
		/** (5, 4, 100): 67 sec. */
		/** (5, 4, 30): 29 sec. */
		/** (5, 5, 30): 46 sec. */
		final int m = 5;
		final int n = 5;
		final int k = 30;
		LOGGER.info("Started.");
		final Run run = Runner.run(pessimisticFactory.get(), m, n, k);
		LOGGER.info("Time: {}.", run.getTotalTimeMs());
//		showRun(run);
		summarizeRun(run);
	}

	public static void showRun(Run run) {
		for (int i = 0; i < run.getK(); ++i) {
			LOGGER.info("Regret after {} questions: {}.", i,
					run.getMinimalMaxRegrets().get(i).getMinimalMaxRegretValue());
			LOGGER.info("Question {}: {}.", i, run.getQuestions().get(i));
		}
		LOGGER.info("Regret after {} questions: {}.", run.getK(),
				run.getMinimalMaxRegrets().get(run.getK()).getMinimalMaxRegretValue());
	}

	public static void summarizeRun(Run run) {
		final ImmutableMap<Integer, Double> everyFive = IntStream.rangeClosed(0, run.getK()).filter(i -> i % 5 == 0)
				.boxed().collect(ImmutableMap.toImmutableMap(i -> i,
						i -> run.getMinimalMaxRegrets().get(i).getMinimalMaxRegretValue()));
		LOGGER.info("Regrets: {}.", everyFive);
	}
}

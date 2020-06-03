package io.github.oliviercailloux.minimax.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.minimax.strategies.MmrOperator;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class TimingXp {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(TimingXp.class);

	public static void main(String[] args) throws Exception {
		final StrategyFactory pessimisticFactory = StrategyFactory.byMmrs(MmrOperator.MAX);
		/** (5, 5, 1), Saucisson: 0.77 ± 0.03 sec. */
		/** (5, 5, 30), Saucisson: 18.2 ± 1.1 sec. */
		final int m = 5;
		final int n = 5;
		final int k = 30;
		/** Warm up the VM. */
		for (int i = 0; i < 1; ++i) {
			final Run run = Runner.run(pessimisticFactory.get(), m, n, k);
			LOGGER.info("Time: {}.", run.getTotalTimeMs());
		}
		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		for (int i = 0; i < 5; ++i) {
			final Run run = Runner.run(pessimisticFactory.get(), m, n, k);
			LOGGER.info("Time: {}.", run.getTotalTimeMs());
			runsBuilder.add(run);
		}
		final Runs runs = Runs.of(runsBuilder.build());
		LOGGER.info("Timing statistics for finding first question: {}.", runs.getQuestionTimeStats().get(0));
		LOGGER.info("Timing statistics for finding all questions: {}.", runs.getTotalTimeStats());
	}
}

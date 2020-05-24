package io.github.oliviercailloux.minimax.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.oliviercailloux.minimax.strategies.MmrOperator;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class PessimisticXp {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(PessimisticXp.class);

	public static void main(String[] args) {
		final StrategyFactory pessimisticFactory = StrategyFactory.aggregatingMmrs(MmrOperator.MAX);
		/** (4, 4, 100), Saucisson: 21 sec; need about 19 q to reach 0. */
		/** (5, 4, 100), Saucisson: 67 sec. */
		/** (5, 4, 30), Saucisson: 29 sec. */
		/** (5, 5, 30), Saucisson: 44 sec. */
		final int m = 5;
		final int n = 5;
		final int k = 30;
		LOGGER.info("Started.");
		final Run run = Runner.run(pessimisticFactory.get(), m, n, k);
		LOGGER.info("Time: {}.", run.getTotalTimeMs());
		Runner.show(run);
		Runner.summarize(run);
	}
}

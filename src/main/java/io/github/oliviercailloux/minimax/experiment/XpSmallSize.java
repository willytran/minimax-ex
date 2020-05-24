package io.github.oliviercailloux.minimax.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class XpSmallSize {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(XpSmallSize.class);

	public static void main(String[] args) {
		final int m = 5;
		final int n = 5;
		final int k = 30;
//		runs(StrategyFactory.aggregatingMmrs(MmrOperator.MAX), m, n, k, 5);
		runs(StrategyFactory.random(), m, n, k, 5);
	}

	/**
	 * Repeat (nbRuns times) a run experiment (thus: generate an oracle, ask k
	 * questions).
	 */
	private static Runs runs(StrategyFactory factory, int m, int n, int k, int nbRuns) {
		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		LOGGER.info("Started {}.", factory.getDescription());
		for (int i = 0; i < nbRuns; ++i) {
			final Run run = Runner.run(factory.get(), m, n, k);
			LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
			runsBuilder.add(run);
		}
		final Runs runs = Runs.of(runsBuilder.build());
		Runner.summarize(runs);
		return runs;
	}
}

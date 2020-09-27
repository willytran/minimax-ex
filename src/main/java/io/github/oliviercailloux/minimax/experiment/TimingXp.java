package io.github.oliviercailloux.minimax.experiment;

import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.minimax.strategies.MmrLottery;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class TimingXp {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(TimingXp.class);

	public static void main(String[] args) throws Exception {
		final long seed = ThreadLocalRandom.current().nextLong();
		final StrategyFactory factory = StrategyFactory.byMmrs(seed, MmrLottery.MAX_COMPARATOR);
		/** (Pessimistic, 5, 5, 1), Saucisson: 0.77 ± 0.03 sec. */
		/** (Pessimistic, 5, 5, 30), Saucisson: 18.2 ± 1.1 sec. */
		/** (Pessimistic, 5, 5, 30), BriBri: 13.7 ± 2.9 sec. */
		final int m = 5;
		final int n = 5;
		final int k = 30;
		/** Warm up the VM. */
		for (int i = 0; i < 1; ++i) {
			final Run run = Runner.run(factory, m, n, k);
			LOGGER.info("Time warm up: {}.", run.getTotalTimeMs());
		}
		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		for (int i = 0; i < 5; ++i) {
			final Run run = Runner.run(factory, m, n, k);
			LOGGER.info("Time run {}: {}.", i, run.getTotalTimeMs());
			runsBuilder.add(run);
		}
		final Runs runs = Runs.of(factory, runsBuilder.build());
		LOGGER.info("Timing statistics for finding an average question: {}.", runs.getQuestionTimeStats());
		LOGGER.info("Timing statistics for finding all questions: {}.", runs.getTotalTimeStats());
	}
}

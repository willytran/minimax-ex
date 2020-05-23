package io.github.oliviercailloux.minimax.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.minimax.regret.Regrets;
import io.github.oliviercailloux.minimax.strategies.MmrOperator;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class PessimisticXp {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(PessimisticXp.class);

	public static void main(String[] args) {
		final StrategyFactory pessimisticFactory = StrategyFactory.aggregatingMmrs(MmrOperator.MAX);
		final int m = 3;
		final int n = 2;
		final int k = 1;
		final Run run = Runner.run(pessimisticFactory.get(), m, n, k);
		LOGGER.info("Regrets: {}.", run.getMinimalMaxRegrets().stream().map(Regrets::getMinimalMaxRegretValue)
				.collect(ImmutableList.toImmutableList()));
	}
}

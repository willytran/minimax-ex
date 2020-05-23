package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Map;
import java.util.function.Supplier;

import io.github.oliviercailloux.minimax.Strategy;
import io.github.oliviercailloux.minimax.StrategyByMmr;
import io.github.oliviercailloux.minimax.StrategyPessimisticHeuristic;
import io.github.oliviercailloux.minimax.StrategyRandom;
import io.github.oliviercailloux.minimax.StrategyTwoPhasesHeuristic;
import io.github.oliviercailloux.minimax.StrategyType;
import io.github.oliviercailloux.minimax.utils.MmrOperator;

public class StrategyFactory implements Supplier<Strategy> {
	public static StrategyFactory given(StrategyType family, Map<String, Object> parameters) {
		switch (family) {
		case PESSIMISTIC:
			return aggregatingMmrs((MmrOperator) parameters.get("MMR operator"));
		case PESSIMISTIC_HEURISTIC:
		case RANDOM:
		case TWO_PHASES_HEURISTIC:
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static StrategyFactory aggregatingMmrs(MmrOperator mmrOperator) {
		return new StrategyFactory(() -> StrategyByMmr.build(mmrOperator), "By MMR " + mmrOperator);
	}

	public static StrategyFactory pessimisticHeuristic() {
		return new StrategyFactory(() -> StrategyPessimisticHeuristic.build(), "By MMR limited");
	}

	public static StrategyFactory random() {
		return new StrategyFactory(() -> StrategyRandom.build(), "Random");
	}

	public static StrategyFactory twoPhases(int questionsToVoters, int questionsToCommittee, boolean committeeFirst) {
		return new StrategyFactory(
				() -> StrategyTwoPhasesHeuristic.build(questionsToVoters, questionsToCommittee, committeeFirst),
				"Two phases " + "qV: " + questionsToVoters + "; qC: " + questionsToCommittee + "; committee first? "
						+ committeeFirst);
	}

	private final Supplier<Strategy> supplier;
	private final String description;

	private StrategyFactory(Supplier<Strategy> supplier, String description) {
		this.supplier = checkNotNull(supplier);
		this.description = checkNotNull(description);
	}

	@Override
	public Strategy get() {
		final Strategy instance = supplier.get();
		checkState(instance != null);
		return instance;
	}

	/**
	 * Returns a short description identifying this factory uniquely.
	 */
	@Override
	public String toString() {
		return description;
	}

}

package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.function.Supplier;

import io.github.oliviercailloux.minimax.Strategy;
import io.github.oliviercailloux.minimax.StrategyByMmr;
import io.github.oliviercailloux.minimax.StrategyType;
import io.github.oliviercailloux.minimax.utils.MmrOperator;

public class StrategyFactory implements Supplier<Strategy> {
	public static StrategyFactory given(StrategyType family, Map<String, Object> parameters) {
		switch (family) {
		case PESSIMISTIC:
			return aggregatingMmrs((MmrOperator) parameters.get("Mmr operator"));
		case PESSIMISTIC_HEURISTIC:
//			return "LimitedPess";
		case RANDOM:
//			return "Random";
		case TWO_PHASES_RANDOM:
//			return "TwoPhasesRandom";
		case TWO_PHASES_HEURISTIC:
//			return "TwoPhases";
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static StrategyFactory aggregatingMmrs(MmrOperator mmrOperator) {
		return new StrategyFactory(() -> StrategyByMmr.build(mmrOperator), "Pessimistic " + mmrOperator);
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
		return checkNotNull(instance);
	}

	/**
	 * Returns a short description identifying this factory uniquely.
	 */
	@Override
	public String toString() {
		return description;
	}

}

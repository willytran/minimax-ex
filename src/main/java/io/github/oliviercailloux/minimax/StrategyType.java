package io.github.oliviercailloux.minimax;

public enum StrategyType {

	MINIMAX_MIN, MINIMAX_AVG, MINIMAX_WEIGHTED_AVG, RANDOM, TWO_PHASES, EXTREME_COMPLETION;

	@Override
	public String toString() {
		switch (this) {
		case MINIMAX_MIN:
			return "Pessimistic Strategy with min aggregation operator";
		case MINIMAX_AVG:
			return "Pessimistic Strategy with avg aggregation operator";
		case MINIMAX_WEIGHTED_AVG:
			return "Pessimistic Strategy with weighted average aggregation operator";
		case RANDOM:
			return "Random Strategy";
		case TWO_PHASES:
			return "Two Phases Strategy";
		case EXTREME_COMPLETION:
			return "Extreme Completion Strategy";
		default:
			throw new IllegalStateException();
		}
	}
	
}

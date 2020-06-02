package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

public class StrategyFactory implements Supplier<Strategy> {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyFactory.class);

	static StrategyFactory given(StrategyType family, Map<String, Object> parameters) {
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
		final Random random = getRandom();
		return new StrategyFactory(() -> {
			final StrategyByMmr strategy = StrategyByMmr.build(mmrOperator);
			strategy.setRandom(random);
			return strategy;
		}, "By MMR " + mmrOperator);
	}

	public static StrategyFactory pessimisticHeuristic() {
		return new StrategyFactory(() -> StrategyPessimisticHeuristic.build(), "By MMR limited");
	}

	public static StrategyFactory random() {
		final Random random = getRandom();
		return new StrategyFactory(() -> {
			final StrategyRandom strategy = StrategyRandom.build();
			strategy.setRandom(random);
			return strategy;
		}, "Random");
	}

	public static StrategyFactory twoPhases(int questionsToVoters, int questionsToCommittee, boolean committeeFirst) {
		return new StrategyFactory(
				() -> StrategyTwoPhasesHeuristic.build(questionsToVoters, questionsToCommittee, committeeFirst),
				"Two phases " + "qV: " + questionsToVoters + "; qC: " + questionsToCommittee + "; committee first? "
						+ committeeFirst);
	}

	private static Random getRandom() {
		final long seed = ThreadLocalRandom.current().nextLong();
		LOGGER.info("Using seed {} as random source.", seed);
		return new Random(seed);
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
	 * Returns a short description of this factory (omits the seed).
	 */
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
//		TODO return MoreObjects.toStringHelper(this).add("Description", description).add("Seed", seed).toString();
		return MoreObjects.toStringHelper(this).add("Description", description).toString();
	}

}

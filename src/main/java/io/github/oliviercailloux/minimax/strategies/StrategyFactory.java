package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.strategies.StrategyByMmr.QuestioningConstraint;

/**
 * Immutable.
 */
public class StrategyFactory implements Supplier<Strategy> {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyFactory.class);

	static StrategyFactory given(StrategyType family, Map<String, Object> parameters) {
		switch (family) {
		case PESSIMISTIC:
			return byMmrs((long) parameters.get("seed"), (MmrOperator) parameters.get("mmrOperator"));
		case PESSIMISTIC_HEURISTIC:
		case RANDOM:
		case TWO_PHASES_HEURISTIC:
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static StrategyFactory pessimistic() {
		final long seed = ThreadLocalRandom.current().nextLong();
		return byMmrs(seed, MmrOperator.MAX);
	}

	public static StrategyFactory byMmrs(long seed, MmrOperator mmrOperator) {
		final Random random = new Random(seed);
		return new StrategyFactory(() -> {
			final StrategyByMmr strategy = StrategyByMmr.build(mmrOperator);
			strategy.setRandom(random);
			return strategy;
		}, mmrOperator.equals(MmrOperator.MAX) ? "Pessimistic" : "By MMR " + mmrOperator);
	}

	public static StrategyFactory limited() {
		final long seed = ThreadLocalRandom.current().nextLong();
		return limited(seed, ImmutableList.of());
	}

	public static StrategyFactory limitedCommitteeThenVoters(int nbQuestionsToCommittee) {
		final QuestioningConstraint cConstraint = StrategyByMmr.QuestioningConstraint
				.of(QuestionType.COMMITTEE_QUESTION, nbQuestionsToCommittee);
		final QuestioningConstraint vConstraint = StrategyByMmr.QuestioningConstraint.of(QuestionType.VOTER_QUESTION,
				Integer.MAX_VALUE);
		final long seed = ThreadLocalRandom.current().nextLong();
		return limited(seed, ImmutableList.of(cConstraint, vConstraint));
	}

	public static StrategyFactory limitedVotersThenCommittee(int nbQuestionsToVoters) {
		final QuestioningConstraint vConstraint = StrategyByMmr.QuestioningConstraint.of(QuestionType.VOTER_QUESTION,
				nbQuestionsToVoters);
		final QuestioningConstraint cConstraint = StrategyByMmr.QuestioningConstraint
				.of(QuestionType.COMMITTEE_QUESTION, Integer.MAX_VALUE);
		final long seed = ThreadLocalRandom.current().nextLong();
		return limited(seed, ImmutableList.of(vConstraint, cConstraint));
	}

	public static StrategyFactory limited(long seed, List<QuestioningConstraint> constraints) {
		final String prefix = ", constrained to [";
		final String suffix = "]";
		final String constraintsDescription = constraints.stream()
				.map(c -> (c.getNumber() == Integer.MAX_VALUE ? "∞" : c.getNumber())
						+ (c.getKind() == QuestionType.COMMITTEE_QUESTION ? "c" : "v"))
				.collect(Collectors.joining(", ", prefix, suffix));

		final Random random = new Random(seed);

		return new StrategyFactory(() -> {
			final StrategyByMmr strategy = StrategyByMmr.limited(constraints);
			strategy.setRandom(random);
			return strategy;
		}, "Limited" + constraintsDescription);
	}

	@Deprecated
	public static StrategyFactory pessimisticHeuristic() {
		return new StrategyFactory(() -> StrategyPessimisticHeuristic.build(), "Limited old");
	}

	public static StrategyFactory random() {
		final Random random = getRandom();
		return new StrategyFactory(() -> {
			final StrategyRandom strategy = StrategyRandom.build();
			strategy.setRandom(random);
			return strategy;
		}, "Random");
	}

	@Deprecated
	public static StrategyFactory twoPhases(int questionsToVoters, int questionsToCommittee, boolean committeeFirst) {
		return new StrategyFactory(
				() -> StrategyTwoPhasesHeuristic.build(questionsToVoters, questionsToCommittee, committeeFirst),
				"Two phases " + "qV: " + questionsToVoters + "; qC: " + questionsToCommittee + "; committee first? "
						+ committeeFirst);
	}

	public static Random getRandom() {
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

package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Range;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.minimax.regret.Regrets;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyHelper {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyHelper.class);

	public static ImmutableSet<EndpointPair<Alternative>> getIncomparablePairs(Graph<Alternative> graph) {
		final Set<Alternative> alternatives = graph.nodes();
		final ImmutableSet.Builder<EndpointPair<Alternative>> builder = ImmutableSet.builder();
		for (Alternative a1 : alternatives) {
			final Stream<Alternative> incomparable = getIncomparables(graph, a1);
			incomparable.forEach(a2 -> builder.add(EndpointPair.unordered(a1, a2)));
		}
		return builder.build();
	}

	public static Stream<Alternative> getIncomparables(Graph<Alternative> graph, Alternative a1) {
		final Set<Alternative> known = graph.adjacentNodes(a1);
		return graph.nodes().stream().filter(a2 -> !known.contains(a2)).filter(a2 -> !a2.equals(a1));
	}

	public static <K, V extends Comparable<V>> ImmutableSet<K> getMinimalElements(Map<K, ? extends V> elements) {
		return getMinimalElements(elements, Comparator.naturalOrder());
	}

	public static <K, V> ImmutableSet<K> getMinimalElements(Map<K, ? extends V> elements, Comparator<V> comparator) {
		final ImmutableSetMultimap<V, K> keysByValue = elements.keySet().stream()
				.collect(ImmutableSetMultimap.toImmutableSetMultimap(elements::get, k -> k));
		final Optional<V> minValueOpt = keysByValue.keySet().stream().min(comparator);
		final ImmutableSet<K> minKeys = minValueOpt.isEmpty() ? ImmutableSet.of() : keysByValue.get(minValueOpt.get());
		verify(minKeys.isEmpty() == keysByValue.isEmpty());
		return minKeys;
	}

	public static StrategyHelper newInstance() {
		return new StrategyHelper();
	}

	private PrefKnowledge knowledge;
	private Random random;

	private StrategyHelper() {
		knowledge = null;
		random = null;
	}

	public PrefKnowledge getKnowledge() {
		checkState(knowledge != null);
		return knowledge;
	}

	public void setKnowledge(PrefKnowledge knowledge) {
		checkArgument(checkNotNull(knowledge).getAlternatives().size() >= 2);
		this.knowledge = knowledge;
	}

	public int getAndCheckM() {
		final int m = getM();
		verify(m >= 2);
		if (m == 2) {
			checkState(!getKnowledge().isProfileComplete());
		}
		return m;
	}

	private int getM() {
		return getKnowledge().getAlternatives().size();
	}

	public Random getRandom() {
		if (random == null) {
			final long seed = ThreadLocalRandom.current().nextLong();
			LOGGER.debug("Random uses seed {}.", seed);
			random = new Random(seed);
		}
		return random;
	}

	public void setRandom(Random random) {
		this.random = checkNotNull(random);
	}

	public int nextInt(int size) {
		return getRandom().nextInt(size);
	}

	public <E> E draw(ImmutableSet<E> candidates) {
		checkArgument(!candidates.isEmpty());
		final int i = getRandom().nextInt(candidates.size());
		return candidates.asList().get(i);
	}

	public ImmutableSet<Voter> getQuestionableVoters() {
		return getKnowledge().getVoters().stream().filter(v -> getKnowledge().getPartialPreference(v)
				.asTransitiveGraph().edges().size() != getM() * (getM() - 1) / 2)
				.collect(ImmutableSet.toImmutableSet());
	}

	public ImmutableSet<QuestionVoter> getPossibleVoterQuestions() {
		return getKnowledge().getVoters().stream().flatMap(this::getPossibleVoterQuestions)
				.collect(ImmutableSet.toImmutableSet());
	}

	private Stream<QuestionVoter> getPossibleVoterQuestions(Voter voter) {
		final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();
		final Stream<QuestionVoter> possibleVoterQuestions = getIncomparablePairs(graph).stream()
				.map(p -> QuestionVoter.given(voter, p.nodeU(), p.nodeV()));
		return possibleVoterQuestions;
	}

	private ImmutableSet<Integer> getRanksWithLambdaRangesWiderThan(double threshold) {
		return IntStream.rangeClosed(1, getM() - 2).boxed().filter(r -> getWidthOfLambdaRangeAtRank(r) > threshold)
				.collect(ImmutableSet.toImmutableSet());
	}

	private double getWidthOfLambdaRangeAtRank(int rank) {
		checkArgument(rank >= 1);
		checkArgument(rank <= getM() - 2);
		final Range<Aprational> lambdaRange = getKnowledge().getLambdaRange(rank);
		return (lambdaRange.upperEndpoint().subtract(lambdaRange.lowerEndpoint())).doubleValue();
	}

	public ImmutableSet<QuestionCommittee> getQuestionsAboutLambdaRangesWiderThan(double threshold) {
		return getRanksWithLambdaRangesWiderThan(threshold).stream().map(this::getQuestionAboutHalfRange)
				.collect(ImmutableSet.toImmutableSet());
	}

	/**
	 * @return an empty set iff m == 2.
	 */
	public ImmutableSet<QuestionCommittee> getQuestionsAboutLambdaRangesWiderThanOrAll(double threshold) {
		final ImmutableSet<QuestionCommittee> questionsThreshold = getQuestionsAboutLambdaRangesWiderThan(threshold);
		final ImmutableSet<QuestionCommittee> questions;
		if (questionsThreshold.isEmpty()) {
			questions = getQuestionsAboutLambdaRangesWiderThan(0d);
		} else {
			questions = questionsThreshold;
		}
		if (getM() > 2) {
			/**
			 * This fails iff the knowledge includes exact points for all lambdas
			 * (equivalently, if all lambda ranges are empty). Considering the way our
			 * experiments are currently designed, we assume this has zero probability.
			 */
			verify(!questions.isEmpty());
		}
		return questions;
	}

	public QuestionCommittee getQuestionAboutHalfRange(int rank) {
		final Range<Aprational> lambdaRange = getKnowledge().getLambdaRange(rank);
		final Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
				.divide(new Apint(2));
		return QuestionCommittee.given(avg, rank);
	}

	public RegretComputer getRegretComputer() {
		return new RegretComputer(getKnowledge());
	}

	public Regrets getMinimalMaxRegrets() {
		return getRegretComputer().getMinimalMaxRegrets();
	}
}

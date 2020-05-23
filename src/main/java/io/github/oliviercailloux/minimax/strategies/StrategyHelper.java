package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyHelper {
	public static ImmutableSet<EndpointPair<Alternative>> getIncomparablePairs(Graph<Alternative> graph) {
		final Set<Alternative> alternatives = graph.nodes();
		final ImmutableSet.Builder<EndpointPair<Alternative>> builder = ImmutableSet.builder();
		for (Alternative a1 : alternatives) {
			final Set<Alternative> known = graph.adjacentNodes(a1);
			alternatives.stream().filter(a2 -> !known.contains(a2)).filter(a2 -> !a2.equals(a1))
					.forEach(a2 -> builder.add(EndpointPair.unordered(a1, a2)));
		}
		return builder.build();
	}

	public static StrategyHelper newInstance() {
		return new StrategyHelper();
	}

	public static StrategyHelper using(PrefKnowledge knowledge) {
		final StrategyHelper strategyHelper = new StrategyHelper();
		strategyHelper.setKnowledge(knowledge);
		return strategyHelper;
	}

	private PrefKnowledge knowledge;
	private final Random random;

	public StrategyHelper() {
		knowledge = null;
		random = new Random();
	}

	public PrefKnowledge getKnowledge() {
		checkState(knowledge != null);
		return knowledge;
	}

	public void setKnowledge(PrefKnowledge knowledge) {
		checkArgument(checkNotNull(knowledge).getAlternatives().size() >= 2);
		this.knowledge = knowledge;
	}

	public ImmutableSet<QuestionVoter> getPossibleVoterQuestions() {
		return knowledge.getVoters().stream().flatMap(this::getPossibleVoterQuestions)
				.collect(ImmutableSet.toImmutableSet());
	}

	private Stream<QuestionVoter> getPossibleVoterQuestions(Voter voter) {
		final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();
		final Stream<QuestionVoter> possibleVoterQuestions = getIncomparablePairs(graph).stream()
				.map(p -> QuestionVoter.given(voter, p.nodeU(), p.nodeV()));
		return possibleVoterQuestions;
	}

	private ImmutableSet<Integer> getRanksWithLambdaRangesWiderThan(double threshold) {
		return IntStream.rangeClosed(1, knowledge.getAlternatives().size() - 2).boxed()
				.filter(r -> getWidthOfLambdaRangeAtRank(r) > threshold).collect(ImmutableSet.toImmutableSet());
	}

	private double getWidthOfLambdaRangeAtRank(int rank) {
		checkArgument(rank >= 1);
		checkArgument(rank <= knowledge.getAlternatives().size() - 2);
		final Range<Aprational> lambdaRange = knowledge.getLambdaRange(rank);
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
		if (knowledge.getAlternatives().size() > 2) {
			/**
			 * This fails iff the knowledge includes exact points for all lambdas
			 * (equivalently, if all lambda ranges are empty). Considering the way our
			 * experiments are currently designed, we assume this has zero probability.
			 */
			verify(!questions.isEmpty());
		}
		return questions;
	}

	private QuestionCommittee getQuestionAboutHalfRange(int rank) {
		final Range<Aprational> lambdaRange = knowledge.getLambdaRange(rank);
		final Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
				.divide(new Apint(2));
		return QuestionCommittee.given(avg, rank);
	}

	public int getAndCheckSize() {
		final int m = getKnowledge().getAlternatives().size();
		verify(m >= 2);
		if (m == 2) {
			checkState(!getKnowledge().isProfileComplete());
		}
		return m;
	}

	public int nextInt(int size) {
		return random.nextInt(size);
	}
}

package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.regret.PairwiseMaxRegret;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyTwoPhasesRandom implements Strategy {

	private PrefKnowledge knowledge;
	public boolean profileCompleted;
	private static boolean weightsFirst;
	private Random random;
	private static int nbComQuest;
	private static int nbVotQuest;
	private static int m;

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyTwoPhasesRandom.class);

	public static StrategyTwoPhasesRandom build(int nbCommitteeQuestions, int nbVotersQuestions,
			boolean committeeFirst) {
		nbComQuest = nbCommitteeQuestions;
		nbVotQuest = nbVotersQuestions;
		weightsFirst = committeeFirst;
		return new StrategyTwoPhasesRandom();
	}

	public static StrategyTwoPhasesRandom build(int nbCommitteeQuestions, int nbVotersQuestions) {
		nbComQuest = nbCommitteeQuestions;
		nbVotQuest = nbVotersQuestions;
		weightsFirst = true;
		return new StrategyTwoPhasesRandom();
	}

	private StrategyTwoPhasesRandom() {
		final long seed = ThreadLocalRandom.current().nextLong();
		LOGGER.info("Using seed: {}.", seed);
		random = new Random(seed);
		profileCompleted = false;
	}

	void setRandom(Random random) {
		this.random = requireNonNull(random);
	}

	/**
	 * Returns the next question that this strategy thinks is best asking.
	 * 
	 * @return a question, or null if (1) there are no more questions or (2) if the
	 *         number of questions to be asked is reached. E.g. Case (1): we ask x
	 *         questions to the committee and then we want to ask y questions to the
	 *         voters, but the maximum number of questions we can ask to the voter
	 *         is less than y. A particular case of (1) is when the number x of
	 *         questions that we want to ask to the committee is 0. Case (2): we
	 *         want to ask y questions to the voters and then x questions to the
	 *         committee, but the number of questions we can ask to the voter is w <
	 *         y. What we do is to ask w questions to the voters and then proceed
	 *         with the x questions to the committee. The number of total questions
	 *         in both cases is less than x+y.
	 * 
	 * @throws IllegalArgumenteException if there are less than two alternatives.
	 */
	@Override
	public Question nextQuestion() {
		checkArgument(m >= 2, "Questions can be asked only if there are at least two alternatives.");
		Question q = null;

		if (nbVotQuest != 0 || nbComQuest != 0) {

			if (weightsFirst) {
				if (nbComQuest > 0) {
					q = questionToCom();
					nbComQuest--;
				} else {
					if (nbVotQuest > 0) {
						q = questionToVot();
						if (q != null) {
							nbVotQuest--;
						} else {
							nbVotQuest = 0;
						}
					}
				}
			} else {
				if (nbVotQuest > 0) {
					q = questionToVot();
					if (q != null) {
						nbVotQuest--;
					} else {
						nbVotQuest = 0;
						q = questionToCom();
						nbComQuest--;
					}
				} else {
					if (nbComQuest > 0) {
						q = questionToCom();
						nbComQuest--;
					}
				}
			}
		}
		return q;
	}

	private Question questionToVot() {
		final ImmutableSet.Builder<Voter> questionableVotersBuilder = ImmutableSet.builder();
		for (Voter voter : knowledge.getVoters()) {
			final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();
			if (graph.edges().size() != m * (m - 1) / 2) {
				questionableVotersBuilder.add(voter);
			}
		}
		final ImmutableSet<Voter> questionableVoters = questionableVotersBuilder.build();
		Question q = null;

		if (!questionableVoters.isEmpty()) {
			final int idx = random.nextInt(questionableVoters.size());
			final Voter voter = questionableVoters.asList().get(idx);
			final ArrayList<Alternative> altsRandomOrder = new ArrayList<>(knowledge.getAlternatives());
			Collections.shuffle(altsRandomOrder, random);
			final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();
			final Optional<Alternative> withIncomparabilities = altsRandomOrder.stream()
					.filter((a1) -> graph.adjacentNodes(a1).size() != m - 1).findAny();
			assert withIncomparabilities.isPresent();
			final Alternative a1 = withIncomparabilities.get();
			final Optional<Alternative> incomparable = altsRandomOrder.stream()
					.filter((a2) -> !a1.equals(a2) && !graph.adjacentNodes(a1).contains(a2)).findAny();
			assert incomparable.isPresent();
			final Alternative a2 = incomparable.get();

			q = Question.toVoter(voter, a1, a2);
		} else {
			profileCompleted = true;
		}
		return q;
	}

	private Question questionToCom() {
		final ArrayList<Integer> candidateRanks = IntStream.rangeClosed(1, m - 2).boxed()
				.collect(Collectors.toCollection(ArrayList::new));
		Collections.shuffle(candidateRanks, random);
		QuestionCommittee qc = null;
		for (int rank : candidateRanks) {
			final Range<Aprational> lambdaRange = knowledge.getLambdaRange(rank);
			final Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
					.divide(new Apint(2));
			qc = QuestionCommittee.given(avg, rank);

		}
		return Question.toCommittee(qc);
	}

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		this.knowledge = knowledge;
		m = knowledge.getAlternatives().size();
	}

	@Override
	public String toString() {
		return "TwoPhRandom";
	}

}
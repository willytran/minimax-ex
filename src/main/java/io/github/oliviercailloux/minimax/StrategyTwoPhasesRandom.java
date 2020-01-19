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
import com.google.common.graph.Graph;

import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
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

	public static StrategyTwoPhasesRandom build(PrefKnowledge knowledge, int nbCommitteeQuestions,
			int nbVotersQuestions, boolean committeeFirst) {
		nbComQuest = nbCommitteeQuestions;
		nbVotQuest = nbVotersQuestions;
		weightsFirst = committeeFirst;
		return new StrategyTwoPhasesRandom(knowledge);
	}

	public static StrategyTwoPhasesRandom build(PrefKnowledge knowledge, int nbCommitteeQuestions,
			int nbVotersQuestions) {
		nbComQuest = nbCommitteeQuestions;
		nbVotQuest = nbVotersQuestions;
		weightsFirst = true;
		return new StrategyTwoPhasesRandom(knowledge);
	}

	private StrategyTwoPhasesRandom(PrefKnowledge knowledge) {
		final long seed = ThreadLocalRandom.current().nextLong();
		LOGGER.info("Using seed: {}.", seed);
		random = new Random(seed);
		this.knowledge = knowledge;
		m = knowledge.getAlternatives().size();
		profileCompleted = false;
	}

	void setRandom(Random random) {
		this.random = requireNonNull(random);
	}

	@Override
	public Question nextQuestion() {
		Question nextQ;
		checkArgument(m >= 2, "Questions can be asked only if there are at least two alternatives.");

		if (weightsFirst) {
			if (nbComQuest > 0) {
				nextQ = questionToCom();
				nbComQuest--;
			} else if (nbVotQuest > 0) {
				nextQ = questionToVot();
				nbVotQuest--;
			} else
				throw new IllegalStateException("No more questions allowed");
		} else {
			if (nbVotQuest > 0) {
				nextQ = questionToVot();
				nbVotQuest--;
			} else if (nbComQuest > 0) {
				nextQ = questionToCom();
				nbComQuest--;
			} else
				throw new IllegalStateException("No more questions allowed");
		}
		return nextQ;
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

		final boolean existsQuestionVoters = !questionableVoters.isEmpty();

		checkArgument(existsQuestionVoters, "No question to ask about voters.");

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

		if (!existsQuestionVoters) {
			profileCompleted = true;
		}

		return Question.toVoter(voter, a1, a2);
	}

	private Question questionToCom() {
		final ArrayList<Integer> candidateRanks = IntStream.rangeClosed(1, m - 2).boxed()
				.collect(Collectors.toCollection(ArrayList::new));
		Collections.shuffle(candidateRanks, random);
		QuestionCommittee qc = null;
		for (int rank : candidateRanks) {
			final Range<Aprational> lambdaRange = knowledge.getLambdaRange(rank);
			// LOGGER.info("Range: {}.", lambdaRange);
			if (!lambdaRange.lowerEndpoint().equals(lambdaRange.upperEndpoint())) {
				final Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
						.divide(new Apint(2));
				qc = QuestionCommittee.given(avg, rank);
			}
		}
		final boolean existsQuestionWeight = qc != null;

		checkArgument(existsQuestionWeight, "No question to ask about weights.");

		return Question.toCommittee(qc);
	}

}
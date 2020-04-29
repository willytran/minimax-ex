package io.github.oliviercailloux.minimax;

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

import com.google.common.base.Verify;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyTwoPhasesRandom implements Strategy {

	private PrefKnowledge knowledge;
	private boolean committeeFirst;
	private Random random;
	private static int questionsToCommittee;
	private static int questionsToVoters;

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyTwoPhasesRandom.class);

	public static StrategyTwoPhasesRandom build(int nbCommitteeQuestions, int nbVotersQuestions,
			boolean committeeFirst) {
		return new StrategyTwoPhasesRandom(nbCommitteeQuestions, nbVotersQuestions, committeeFirst);
	}

	public static StrategyTwoPhasesRandom build(int nbCommitteeQuestions, int nbVotersQuestions) {
		return new StrategyTwoPhasesRandom(nbCommitteeQuestions, nbVotersQuestions, true);
	}

	private StrategyTwoPhasesRandom(int nbCommitteeQuestions, int nbVotersQuestions, boolean cFirst) {
		final long seed = ThreadLocalRandom.current().nextLong();
		LOGGER.info("TwoPhRandom. Using seed: {}.", seed);
		random = new Random(seed);
		questionsToCommittee = nbCommitteeQuestions;
		questionsToVoters = nbVotersQuestions;
		committeeFirst = cFirst;
	}

	void setRandom(Random random) {
		this.random = requireNonNull(random);
	}

	/**
	 * Returns the next question that this strategy thinks is best asking.
	 * 
	 * @return a question.
	 * 
	 * @throws VerifyException       if there are less than two alternatives or if
	 *                               there are exactly two alternatives and the
	 *                               profile is complete.
	 * @throws IllegalStateException if the profile is complete and a question for
	 *                               the voters is demanded.
	 */
	@Override
	public Question nextQuestion() {
		final int m = knowledge.getAlternatives().size();
		Verify.verify(m > 2 || (m == 2 && !knowledge.isProfileComplete()));
		assert (questionsToVoters != 0 || questionsToCommittee != 0);
		Question q;

		if (committeeFirst) {
			if (questionsToCommittee > 0) {
				q = questionToCommittee();
				questionsToCommittee--;
			} else {
				q = questionToVoters();
				questionsToVoters--;

			}
		} else {
			if (questionsToVoters > 0) {
				q = questionToVoters();
				questionsToVoters--;

			} else {
				q = questionToCommittee();
				questionsToCommittee--;
			}
		}

		return q;
	}

	private Question questionToVoters() {
		final ImmutableSet.Builder<Voter> questionableVotersBuilder = ImmutableSet.builder();
		final int m = knowledge.getAlternatives().size();
		for (Voter voter : knowledge.getVoters()) {
			final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();
			if (graph.edges().size() != m * (m - 1) / 2) {
				questionableVotersBuilder.add(voter);
			}
		}
		final ImmutableSet<Voter> questionableVoters = questionableVotersBuilder.build();

		if (questionableVoters.isEmpty()) {
			throw new IllegalStateException("The profile is complete and a question to the voters has been demanded.");
		}
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

		return Question.toVoter(voter, a1, a2);
	}

	private Question questionToCommittee() {
		final int m = knowledge.getAlternatives().size();
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
	}

	@Override
	public String toString() {
		return "TwoPhRandom";
	}

	@Override
	public StrategyType getStrategyType() {
		return StrategyType.TWO_PHASES_RANDOM;
	}

}
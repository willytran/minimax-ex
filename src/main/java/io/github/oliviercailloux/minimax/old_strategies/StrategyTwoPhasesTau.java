package io.github.oliviercailloux.minimax.old_strategies;

import static com.google.common.base.Preconditions.checkArgument;

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

import com.google.common.collect.Range;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.minimax.Strategy;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyTwoPhasesTau implements Strategy {

	private PrefKnowledge knowledge;
	public boolean profileCompleted;
	private static boolean weightsFirst;
	private Random random;
	private static int nbComQuest;
	private static int nbVotQuest;
	private static int m;

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyTwoPhasesTau.class);

	public static StrategyTwoPhasesTau build(int nbCommitteeQuestions, int nbVotersQuestions) {
		nbComQuest = nbCommitteeQuestions;
		nbVotQuest = nbVotersQuestions;
		weightsFirst = true;
		return new StrategyTwoPhasesTau();
	}

	public static StrategyTwoPhasesTau build(int nbCommitteeQuestions, int nbVotersQuestions, boolean committeeFirst) {
		nbComQuest = nbCommitteeQuestions;
		nbVotQuest = nbVotersQuestions;
		weightsFirst = committeeFirst;
		return new StrategyTwoPhasesTau();
	}

	private StrategyTwoPhasesTau() {
		profileCompleted = false;
		final long seed = ThreadLocalRandom.current().nextLong();
		LOGGER.info("Using seed: {}.", seed);
		random = new Random(seed);
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
			} else {
				throw new IllegalStateException("No more questions allowed");
			}
		} else {
			if (nbVotQuest > 0) {
				nextQ = questionToVot();
				nbVotQuest--;
			} else if (nbComQuest > 0) {
				nextQ = questionToCom();
				nbComQuest--;
			} else {
				throw new IllegalStateException("No more questions allowed");
			}
		}
		return nextQ;
	}

	private Question questionToCom() {
		QuestionCommittee nextQ = null;
		final ArrayList<Integer> candidateRanks = IntStream.rangeClosed(1, m - 2).boxed()
				.collect(Collectors.toCollection(ArrayList::new));
		Collections.shuffle(candidateRanks, random);
		for (int rank : candidateRanks) {
			final Range<Aprational> lambdaRange = knowledge.getLambdaRange(rank);
			// LOGGER.info("Range: {}.", lambdaRange);
			if (!lambdaRange.lowerEndpoint().equals(lambdaRange.upperEndpoint())) {
				final Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
						.divide(new Apint(2));
				nextQ = QuestionCommittee.given(avg, rank);
			}
		}
		return Question.toCommittee(nextQ);
	}

	private Question questionToVot() {
		Voter uncertainVoter = getMinVoter();
		final ArrayList<Alternative> altsRandomOrder = new ArrayList<>(knowledge.getAlternatives());
		Collections.shuffle(altsRandomOrder, random);
		final Graph<Alternative> graph = knowledge.getProfile().get(uncertainVoter).asTransitiveGraph();
		final Optional<Alternative> withIncomparabilities = altsRandomOrder.stream()
				.filter((a1) -> graph.adjacentNodes(a1).size() != m - 1).findAny();
		if (!withIncomparabilities.isPresent()) {
			System.out.println(knowledge.getPartialPreference(uncertainVoter));
			throw new IllegalStateException("No more voter questions");
		}
		final Alternative a1 = withIncomparabilities.get();
		final Optional<Alternative> incomparable = altsRandomOrder.stream()
				.filter((a2) -> !a1.equals(a2) && !graph.adjacentNodes(a1).contains(a2)).findAny();
		assert incomparable.isPresent();
		final Alternative a2 = incomparable.get();
		return Question.toVoter(uncertainVoter, a1, a2);
	}

	private Voter getMinVoter() {
		Regret.getMMRAlternatives(knowledge);
		Alternative yAdv = Regret.getyAdv();
		Alternative xOpt = Regret.getxOpt();

		Regret.getPMR(xOpt, yAdv, knowledge);
		PSRWeights wBar = knowledge.getConstraintsOnWeights().getLastSolution();

		Voter uncertainVoter = knowledge.getProfile().keySet().asList().get(0);
		double tauVmin = getTauVi(uncertainVoter, xOpt, yAdv, wBar);
		double tauVi;
		System.out.println();
		for (Voter v : knowledge.getProfile().keySet()) {
			tauVi = getTauVi(v, xOpt, yAdv, wBar);
			if (tauVi < tauVmin) {
				tauVmin = tauVi;
				uncertainVoter = v;
			}
			System.out.print("v" + v.getId() + " " + tauVi + " ");
		}
		System.out.print("uncertainVoter" + uncertainVoter.getId() + " " + tauVmin + " ");
		return uncertainVoter;
	}

	private double getTauVi(Voter vi, Alternative xOpt, Alternative yAdv, PSRWeights wBar) {
		int nbAlt = knowledge.getAlternatives().size();
		int[] xrank = new int[nbAlt + 1];
		int[] yrank = new int[nbAlt + 1];
		int[] r;
		for (Voter v : knowledge.getProfile().keySet()) {
			if (v.equals(vi)) {
				r = Regret.getWorstRanks(yAdv, xOpt, knowledge.getProfile().get(v));
				yrank[r[1]]++;
				xrank[r[0]]++;
			} else {
				r = Regret.getWorstRanks(xOpt, yAdv, knowledge.getProfile().get(v));
				xrank[r[0]]++;
				yrank[r[1]]++;
			}
		}
		double regret = 0;
		for (int i = 1; i <= nbAlt; i++) {
			regret += (yrank[i] - xrank[i]) * wBar.getWeightAtRank(i);
		}
		return regret;
	}

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		this.knowledge = knowledge;
		m = knowledge.getAlternatives().size();
	}

}

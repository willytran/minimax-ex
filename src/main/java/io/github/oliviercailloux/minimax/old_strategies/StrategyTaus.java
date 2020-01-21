package io.github.oliviercailloux.minimax.old_strategies;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.jlp.elements.SumTerms;
import io.github.oliviercailloux.jlp.elements.SumTermsBuilder;
import io.github.oliviercailloux.minimax.Strategy;
import io.github.oliviercailloux.minimax.elicitation.ConstraintsOnWeights;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyTaus implements Strategy {
	private PrefKnowledge knowledge;
	private static Voter uncertainVoter;
	private static PSRWeights wMin;
	private static PSRWeights wBar;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyTaus.class);

	public static StrategyTaus build(PrefKnowledge knowledge) {
		return new StrategyTaus(knowledge);
	}

	private StrategyTaus(PrefKnowledge knowledge) {
		this.knowledge = knowledge;
		LOGGER.info("INFO"+System.currentTimeMillis());
	}

	@Override
	public Question nextQuestion() {
		Question nextQ;
		final int m = knowledge.getAlternatives().size();

		checkArgument(m >= 2, "Questions can be asked only if there are at least two alternatives.");

		if (tauWSmallerThanTauV(knowledge)) {
			/** Ask a question to the committee about the most valuable rank */
			int maxRank = 2;
			double maxDiff = Math.abs(wBar.getWeightAtRank(maxRank) - wMin.getWeightAtRank(maxRank));
			
			for (int i = maxRank; i <= m; i++) {
				double diff = Math.abs(wBar.getWeightAtRank(i) - wMin.getWeightAtRank(i));
				if (diff > maxDiff) {
					maxDiff = diff;
					maxRank = i;
				}
//				System.out.println(diff + " " + maxDiff);
//				System.out.println(maxRank);
			}
			Range<Aprational> lambdaRange = knowledge.getLambdaRange(maxRank - 1);
			Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
					.divide(new Apint(2));
			nextQ = Question.toCommittee(QuestionCommittee.given(avg, maxRank - 1));
		} else {
			Random random = new Random();
			final ArrayList<Alternative> altsRandomOrder = new ArrayList<>(knowledge.getAlternatives());
			Collections.shuffle(altsRandomOrder, random);
			final Graph<Alternative> graph = knowledge.getProfile().get(uncertainVoter).asTransitiveGraph();
			final Optional<Alternative> withIncomparabilities = altsRandomOrder.stream()
					.filter((a1) -> graph.adjacentNodes(a1).size() != m - 1).findAny();
			if (!withIncomparabilities.isPresent()) {
				throw new IllegalStateException("No more voter questions");
			}
			final Alternative a1 = withIncomparabilities.get();
			final Optional<Alternative> incomparable = altsRandomOrder.stream()
					.filter((a2) -> !a1.equals(a2) && !graph.adjacentNodes(a1).contains(a2)).findAny();
			assert incomparable.isPresent();
			final Alternative a2 = incomparable.get();
			nextQ = Question.toVoter(uncertainVoter, a1, a2);
		}
//		System.out.println(nextQ);
		return nextQ;
	}

	public static boolean tauWSmallerThanTauV(PrefKnowledge knowledge) {		
		Regret.getMMRAlternatives(knowledge);
		Alternative yAdv = Regret.getyAdv();
		Alternative xOpt = Regret.getxOpt();
		
		Regret.getPMR(xOpt, yAdv, knowledge);
		wBar = knowledge.getConstraintsOnWeights().getLastSolution();

		double tauW = getTauW(knowledge, xOpt, yAdv);
		wMin= knowledge.getConstraintsOnWeights().getLastSolution();
		
		uncertainVoter = knowledge.getProfile().keySet().asList().get(0);
		double tauVmin = getTauVi(knowledge, uncertainVoter, xOpt, yAdv);
		double tauVi;
//		System.out.print("tauW "+ tauW);
		for (Voter v : knowledge.getProfile().keySet()) {
			tauVi = getTauVi(knowledge, v, xOpt, yAdv);
			if (tauVi < tauVmin) {
				tauVmin = tauVi;
				uncertainVoter = v;
			}
//			System.out.print(" tau v"+v.getId()+ " "+tauVi);
		}
//		System.out.println(" + tau v "+ tauVmin+ " v"+uncertainVoter.getId());
		return tauW < tauVmin;
	}

	private static double getTauW(PrefKnowledge knowledge, Alternative xOpt, Alternative yAdv) {
		int nbAlt = knowledge.getAlternatives().size();
		int[] xrank = new int[nbAlt + 1];
		int[] yrank = new int[nbAlt + 1];
		int[] r;
		for (Voter v : knowledge.getProfile().keySet()) {
			r = Regret.getWorstRanks(xOpt, yAdv, knowledge.getProfile().get(v));
			xrank[r[0]]++;
			yrank[r[1]]++;
		}
		ConstraintsOnWeights cow = knowledge.getConstraintsOnWeights();
		SumTermsBuilder sb = SumTerms.builder();
		for (int i = 1; i <= nbAlt; i++) {
			sb.add(cow.getTerm(yrank[i] - xrank[i], i));
		}
		SumTerms objective = sb.build();
		return cow.minimize(objective);
	}

	private static double getTauVi(PrefKnowledge knowledge, Voter vi, Alternative xOpt, Alternative yAdv) {
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

}

package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyExtremeCompletion implements Strategy {
	private PrefKnowledge knowledge;

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyMiniMax.class);

	public static StrategyExtremeCompletion build(PrefKnowledge knowledge) {
		return new StrategyExtremeCompletion(knowledge);
	}

	private StrategyExtremeCompletion(PrefKnowledge knowledge) {
		this.knowledge = knowledge;
	}

	@Override
	public Question nextQuestion() {
		Question nextQ;
		final int m = knowledge.getAlternatives().size();

		checkArgument(m >= 2, "Questions can be asked only if there are at least two alternatives.");

		if (Regret.tau1SmallerThanTau2(knowledge)) {
			/** Ask a question to the committee about the most valuable rank */
			PSRWeights wTau = Regret.getWTau();
			PSRWeights wBar = Regret.getWBar();
			double maxDiff = wBar.getWeightAtRank(2) - wTau.getWeightAtRank(2);
			int maxRank = 2;
			for (int i = 2; i <= m; i++) {
				double diff = Math.abs(wBar.getWeightAtRank(i) - wTau.getWeightAtRank(i));
				if (diff > maxDiff) {
					maxDiff = diff;
					maxRank = i;
				}
				System.out.println(diff + " " + maxDiff);
				System.out.println(maxRank);
			}
			Range<Aprational> lambdaRange = knowledge.getLambdaRange(maxRank - 1);
			Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
					.divide(new Apint(2));
			nextQ = Question.toCommittee(QuestionCommittee.given(avg, maxRank - 1));
		} else {
			Random random = new Random();
			Voter voter = Regret.getCandidateVoter(random.nextBoolean());
			assert voter != null;
			final ArrayList<Alternative> altsRandomOrder = new ArrayList<>(knowledge.getAlternatives());
			Collections.shuffle(altsRandomOrder, random);
			final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();
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
			nextQ = Question.toVoter(voter, a1, a2);
		}
		System.out.println(nextQ);
		return nextQ;
	}

}

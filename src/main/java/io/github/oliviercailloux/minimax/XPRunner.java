package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apfloat.Aprational;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.graph.MutableGraph;
import com.google.common.math.Stats;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.elicitation.Answer;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.minimax.utils.AggregationOperator.AggOps;
import io.github.oliviercailloux.minimax.utils.Rounder;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class XPRunner {

	static Set<Alternative> alternatives;
	static Set<Voter> voters;
	static Oracle context;
	static PrefKnowledge knowledge;
	static double[] sumOfRanks;
	static int k; // number of questions
	static List<Alternative> winners;
	static List<Alternative> trueWinners;
	static double trueWinScore;
	static double avgloss;
	static double regret;
	static List<Double> avglosses;
	static List<Double> regrets;
	static Rounder rounder;

	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {
		int n, m;
		String title;
		String root = Paths.get("").toAbsolutePath() + "/experiments/";

		run(4, 8, "prova", StrategyType.EXTREME_COMPLETION);

//	    m=6;n=4;
//	    System.out.println(m+" "+n);
//	    title = root + "m" + m + "n" + n + "MiniMax_WeightedAvg";
//	    run(m, n, title, StrategyType.MINIMAX_WEIGHTED_AVG);

//		NumberFormat formatter = new DecimalFormat("#0.00");
//		BufferedWriter b = initFile(root+"TimeRegret");
//		b.write("Average time of computing the Regret over 100 runs after m+n questions \n");
//		for (m = 5; m <= 25; m += 5) {
//			for (n = 5; n <= 25; n += 5) {
//				b.write("m="+m+" n="+n+" "+ formatter.format(runRegret(m,n))+ " milliseconds \n");
//				System.out.println("m="+m+" n="+n);
//				b.flush();
//			}
//		}
//		b.close();

//		run(4, 6, "test", StrategyType.CURRENT_SOLUTION);

//		for (m =5; m < 7; m++) {
//			for (n = 3; n < 7; n++) {
//				
////				title = root + "m" + m + "n" + n + "MiniMax_Min";
////				run(m, n, title, StrategyType.MINIMAX_MIN);
////				title = root + "m" + m + "n" + n + "MiniMax_Avg";
////				run(m, n, title, StrategyType.MINIMAX_AVG);
////				title = root + "m" + m + "n" + n + "MiniMax_WeightedAvg";
////				run(m, n, title, StrategyType.MINIMAX_WEIGHTED_AVG);
////				title = root + "m" + m + "n" + n + "Random";
////				run(m, n, title, StrategyType.RANDOM);
////				System.out.println(m +" "+ n);
////				title = root + "m" + m + "n" + n + "TwoPhases";
////				run(m, n, title, StrategyType.TWO_PHASES);
//			}
//		}
	}

	private static void run(int m, int n, String file, StrategyType st) throws IOException {
		final long startTime = System.currentTimeMillis();
		int maxQuestions = 30;
		int runs = 10;
		rounder = Rounder.given(Rounder.Mode.ROUND_HALF_UP, 3);
		BufferedWriter b = initFile(file);
		b.write(st + "\n");
		b.write(n + " Voters " + m + " Alternatives \n");
		b.write(maxQuestions + " Questions for " + runs + " runs \n");
		b.flush();
		int qstVot = 0;
		int qstCom = 0;
		ArrayList<Double> regretSeriesMean = new ArrayList<>();
		ArrayList<Double> avgLossSeriesMean = new ArrayList<>();
		ArrayList<Double> regretSeriesSD = new ArrayList<>();
		ArrayList<Double> avgLossSeriesSD = new ArrayList<>();
		int regret2 = -1, regret4 = -1, regret8 = -1;
		int avgLoss2 = -1, avgLoss4 = -1, avgLoss8 = -1;
		double initialRegret = 1;
		double initialAvgLoss = 1;
		for (int nbquest = 1; nbquest <= maxQuestions; nbquest++) {
			avglosses = new LinkedList<>();
			regrets = new LinkedList<>();
			for (int j = 0; j < runs; j++) {
				alternatives = new HashSet<>();
				for (int i = 1; i <= m; i++) {
					alternatives.add(new Alternative(i));
				}
				voters = new HashSet<>();
				for (int i = 1; i <= n; i++) {
					voters.add(new Voter(i));
				}
				context = Oracle.build(ImmutableMap.copyOf(genProfile(n, m)), genWeights(m));
				knowledge = PrefKnowledge.given(alternatives, voters);
				knowledge.setRounder(rounder);
				Strategy strategy = null;
				switch (st) {
				case MINIMAX_MIN:
					strategy = StrategyMiniMax.build(knowledge, AggOps.MIN);
					break;
				case MINIMAX_AVG:
					strategy = StrategyMiniMax.build(knowledge, AggOps.AVG);
					break;
				case MINIMAX_WEIGHTED_AVG:
					strategy = StrategyMiniMax.build(knowledge, AggOps.WEIGHTED_AVERAGE, 1d,
							context.getWeights().getWeightAtRank(m - 1) / 2 * n);
					break;
				case RANDOM:
					strategy = StrategyRandom.build(knowledge);
					break;
				case TWO_PHASES:
					strategy = StrategyTwoPhases.build(knowledge, AggOps.WEIGHTED_AVERAGE, 1d,
							context.getWeights().getWeightAtRank(m - 1) / 2 * n);
					break;
				case EXTREME_COMPLETION:
					strategy = StrategyExtremeCompletion.build(knowledge);
					break;
				}
				sumOfRanks = new double[m];
				trueWinners = computeTrueWinners();

				for (k = 1; k <= nbquest; k++) {
					Question q;
					try {
						q = strategy.nextQuestion();
						if (q.getType() == QuestionType.COMMITTEE_QUESTION) {
							qstCom++;
						} else {
							qstVot++;
						}
						Answer a = context.getAnswer(q);
						updateKnowledge(q, a);
					} catch (Exception e) {
//						e.printStackTrace();
						break;
					}
				}

				winners = Regret.getMMRAlternatives(knowledge);
				regret = Regret.getMMR();
				regrets.add(regret);
				List<Double> losses = new LinkedList<>();
				for (Alternative alt : winners) {
					double approxTrueScore = 0;
					for (VoterStrictPreference vsp : context.getProfile().values()) {
						int rank = vsp.asStrictPreference().getAlternativeRank(alt);
						approxTrueScore += context.getWeights().getWeightAtRank(rank);
					}
					losses.add(trueWinScore - approxTrueScore);
				}
				avgloss = 0;
				for (double loss : losses) {
					avgloss += loss;
				}
				avgloss = avgloss / losses.size();
				avglosses.add(avgloss);
			}
			Stats regretStats = Stats.of(regrets);
			double regretMean = regretStats.mean();
			double regretSD = regretStats.populationStandardDeviation();
			Stats lossStats = Stats.of(avglosses);
			double lossMean = lossStats.mean();
			double lossSD = lossStats.populationStandardDeviation();
			regretSeriesMean.add(regretMean);
			avgLossSeriesMean.add(lossMean);
			regretSeriesSD.add(regretSD);
			avgLossSeriesSD.add(lossSD);

			if (nbquest == 1) {
				initialRegret = regretMean;
				initialAvgLoss = lossMean;
			}
			if (regretMean <= (initialRegret / 2) && regret2 < 0) {
				regret2 = nbquest;
			}
			if (regretMean <= (initialRegret / 4) && regret4 < 0) {
				regret4 = nbquest;
			}
			if (regretMean <= (initialRegret / 8) && regret8 < 0) {
				regret8 = nbquest;
			}
			if (lossMean <= (initialAvgLoss / 2) && avgLoss2 < 0) {
				avgLoss2 = nbquest;
			}
			if (lossMean <= (initialAvgLoss / 4) && avgLoss4 < 0) {
				avgLoss4 = nbquest;
			}
			if (lossMean <= (initialAvgLoss / 8) && avgLoss8 < 0) {
				avgLoss8 = nbquest;
			}
		}
		b.write("Mean of Regret reduced by half in " + regret2 + " questions \n");
		b.write("Mean of Regret reduced by four in " + regret4 + " questions \n");
		b.write("Mean of Regret reduced by eight in " + regret8 + " questions \n");
		b.write("Mean of Average Loss reduced by half in " + avgLoss2 + " questions \n");
		b.write("Mean of Average Loss reduced by four in " + avgLoss4 + " questions \n");
		b.write("Mean of Average Loss reduced by eight in " + avgLoss8 + " questions \n");
		b.flush();

		b.write("k \t Mean of Regrets \n");
		b.write("0 \t" + initialRegret + "\n");
		for (int i = 1; i <= maxQuestions; i++) {
			b.write(i + "\t" + regretSeriesMean.get(i - 1) + "\n");
		}
		b.flush();

		b.write("k \t Mean of Average Losses \n");
		b.write("0 \t" + initialAvgLoss + "\n");
		for (int i = 1; i <= maxQuestions; i++) {
			b.write(i + "\t" + avgLossSeriesMean.get(i - 1) + "\n");
		}
		b.flush();

		b.write("k \t Standard Deviations of Regrets \n");
		for (int i = 1; i <= maxQuestions; i++) {
			b.write(i + "\t" + regretSeriesSD.get(i - 1) + "\n");
		}
		b.flush();

		b.write("k \t Standard Deviations of Average Losses \n");
		for (int i = 1; i <= maxQuestions; i++) {
			b.write(i + "\t" + regretSeriesSD.get(i - 1) + "\n");
		}
		NumberFormat formatter = new DecimalFormat("#0.00000");
		b.write("Duration " + formatter.format((System.currentTimeMillis() - startTime) / 1000d) + " seconds \n");
		b.write("Questions to the voters: " + qstVot + " Question to the committee: " + qstCom);
		b.flush();
		b.close();
	}


	private static BufferedWriter initFile(String tfile) {
		BufferedWriter b = null;
		try {
			File file = new File(tfile);
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();
			b = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return b;
	}

	private static void updateKnowledge(Question qt, Answer answ) {
		switch (qt.getType()) {
		case VOTER_QUESTION:
			QuestionVoter qv = qt.getQuestionVoter();
			Alternative a = qv.getFirstAlternative();
			Alternative b = qv.getSecondAlternative();
			MutableGraph<Alternative> graph = knowledge.getProfile().get(qv.getVoter()).asGraph();
			switch (answ) {
			case GREATER:
				graph.putEdge(a, b);
				knowledge.getProfile().get(qv.getVoter()).setGraphChanged();
				break;
			case LOWER:
				graph.putEdge(b, a);
				knowledge.getProfile().get(qv.getVoter()).setGraphChanged();
				break;
			// $CASES-OMITTED$
			default:
				throw new IllegalStateException();
			}
			break;
		case COMMITTEE_QUESTION:
			QuestionCommittee qc = qt.getQuestionCommittee();
			Aprational lambda = qc.getLambda();
			int rank = qc.getRank();
			switch (answ) {
			case EQUAL:
				knowledge.addConstraint(rank, ComparisonOperator.EQ, lambda);
				break;
			case GREATER:
				knowledge.addConstraint(rank, ComparisonOperator.GE, lambda);
				break;
			case LOWER:
				knowledge.addConstraint(rank, ComparisonOperator.LE, lambda);
				break;
			default:
				throw new IllegalStateException();
			}
			break;
		default:
			throw new IllegalStateException();
		}
	}

	private static List<Alternative> computeTrueWinners() {
		List<Alternative> trueWin = new LinkedList<>();
		for (int i = 0; i < alternatives.size(); i++) {
			Alternative a = new Alternative(i + 1);
			double sum = 0;
			for (Voter v : voters) {
				int rank = context.getProfile().get(v).asStrictPreference().getAlternativeRank(a);
				sum += context.getWeights().getWeightAtRank(rank);
			}
			sumOfRanks[i] = sum;
		}

		double max = sumOfRanks[0];
		trueWin.add(new Alternative(1));
		for (int i = 1; i < sumOfRanks.length; i++) {
			if (sumOfRanks[i] == max) {
				trueWin.add(new Alternative(i + 1));
			}
			if (sumOfRanks[i] > max) {
				trueWin.clear();
				max = sumOfRanks[i];
				trueWin.add(new Alternative(i + 1));
			}
		}
		trueWinScore = max;
		return trueWin;
	}

	public static PSRWeights genWeights(int nbAlternatives) {
		List<Double> weights = new LinkedList<>();
		weights.add(1d);
		double previous = 1d;
		Random r = new Random();
		double[] differences = new double[nbAlternatives - 1];
		double sum = 0;
		for (int i = 0; i < nbAlternatives - 1; i++) {
			differences[i] = r.nextDouble();
			sum += differences[i];
		}
		for (int i = 0; i < nbAlternatives - 1; i++) {
			differences[i] = differences[i] / sum;
		}
		Arrays.sort(differences);
		for (int i = nbAlternatives - 2; i > 0; i--) {
			double curr = rounder.round(previous - differences[i]); 
			weights.add(curr);
			previous = curr;
		}
		weights.add(0d);
		return PSRWeights.given(weights);
	}

	public static Map<Voter, VoterStrictPreference> genProfile(int nbVoters, int nbAlternatives) {
		checkArgument(nbVoters >= 1);
		checkArgument(nbAlternatives >= 1);
		Map<Voter, VoterStrictPreference> profile = new HashMap<>();

		List<Alternative> availableRanks = new LinkedList<>();
		for (int i = 1; i <= nbAlternatives; i++) {
			availableRanks.add(new Alternative(i));
		}

		for (int i = 1; i <= nbVoters; ++i) {
			Voter v = new Voter(i);
			List<Alternative> linearOrder = Lists.newArrayList(availableRanks);
			Collections.shuffle(linearOrder);
			VoterStrictPreference pref = VoterStrictPreference.given(v, linearOrder);
			profile.put(v, pref);
		}

		return profile;
	}

}

package io.github.oliviercailloux.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

import io.github.oliviercailloux.minimax.Strategy;
import io.github.oliviercailloux.minimax.StrategyTwoPhasesHeuristic;
import io.github.oliviercailloux.minimax.StrategyType;
import io.github.oliviercailloux.minimax.elicitation.Answer;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.utils.Generator;

public class Runner {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
	private static final String root = Paths.get("").toAbsolutePath() + "/experiments/";

	private static int qV = 0;
	private static int qC = 0;
	private static boolean committeeFirst = true;

	public static void main(String[] args) throws IOException {
		final int k = 500; // nbQuestions
		final int nbRuns = 25;
		final int m = 10; // alternatives
		final int n = 20; // agents
		String head = "nbQst = " + k;

//		Strategy stRandom = StrategyRandom.build();
//		runXP(k, nbRuns, m, n, stRandom, head);

//		Strategy stPessimistic = StrategyPessimistic.build(AggOps.MAX);
//		runXP(k, nbRuns, m, n, stPessimistic, head);

		/*
		 * Experiments Table 2
		 * 
		 * Strategy stLimitedPess = StrategyPessimisticHeuristic.build(AggOps.MAX);
		 * runXP(k, nbRuns, 5, 10, stLimitedPess, head); stLimitedPess =
		 * StrategyPessimisticHeuristic.build(AggOps.MAX); runXP(k, nbRuns, 5, 15,
		 * stLimitedPess, head); stLimitedPess =
		 * StrategyPessimisticHeuristic.build(AggOps.MAX); runXP(k, nbRuns, 5, 20,
		 * stLimitedPess, head); stLimitedPess =
		 * StrategyPessimisticHeuristic.build(AggOps.MAX); runXP(800, nbRuns, 10, 20,
		 * stLimitedPess, "nbQst = " +800); stLimitedPess =
		 * StrategyPessimisticHeuristic.build(AggOps.MAX); runXP(800, 10, 10, 30,
		 * stLimitedPess, "nbQst = " +800); stLimitedPess =
		 * StrategyPessimisticHeuristic.build(AggOps.MAX); runXP(800, 10, 15, 30,
		 * stLimitedPess, "nbQst = " +800);
		 */

		Strategy stTwoPhHeuristic;

		for (int i = 0; i <= k; i += 50) {
			qV = i;
			qC = k - i;
			System.out.println(qV);
			stTwoPhHeuristic = StrategyTwoPhasesHeuristic.build(qV, qC, committeeFirst);
			head = "qC = " + qC + " then qV = " + qV;
			try {
				runXP(k, nbRuns, m, n, stTwoPhHeuristic, head);
			} catch (IllegalStateException e) {
				System.out.println("Complete at qV " + qV);
				e.printStackTrace();
				break;
			}
		}

//		committeeFirst = false;
//		for (int i = 0; i <= k; i += 50) {
//			stTwoPhHeuristic = StrategyTwoPhasesHeuristic.build(i, (k - i), committeeFirst);
//			head = "qV = " + i + " then qC = " + (k - i);
//			runXP(k, nbRuns, m, n, stTwoPhHeuristic, head);
//		}
	}

	private static void runXP(int k, int nbRuns, int m, int n, Strategy strategy, String head) throws IOException {
		String title = root + "m" + m + "n" + n + strategy.toString() + "_" + k;
		if (strategy.getStrategyType().equals(StrategyType.TWO_PHASES_HEURISTIC)) {
			title = root + "m" + m + "n" + n + strategy.toString() + "_qV" + qV + "qC" + qC;
		}
		final ImmutableMap.Builder<String, ImmutableList<Double>> builder = ImmutableMap.builder();
		final Runs runs = runRepeatedly(strategy, k, m, n, nbRuns, title);

		System.out.println("Time for the stats:");
		long s = System.currentTimeMillis();
		final ImmutableList<Double> regrets = runs.getAverageMinimalMaxRegrets();
		System.out.println((System.currentTimeMillis() - s) / 1000);

		builder.put(head, regrets);
		final ImmutableMap<String, ImmutableList<Double>> results = builder.build();

		final CsvWriter writer = new CsvWriter(Files.newOutputStream(Path.of(title + "_out.csv")),
				new CsvWriterSettings());
		final ImmutableList<String> headers = Stream.concat(Stream.of("i"), results.keySet().stream())
				.collect(ImmutableList.toImmutableList());
		writer.writeHeaders(headers);

		for (int j = 0; j < k + 1; ++j) {
			writer.addValue(j);
			for (int col = 0; col < results.size(); ++col) {
				double avg;
				if (j < results.values().asList().get(col).size()) {
					avg = results.values().asList().get(col).get(j);
				} else {
					avg = -1;
				}
				writer.addValue(avg);
			}
			writer.writeValuesToRow();
		}
		writer.close();
	}

	private static Runs runRepeatedly(Strategy strategy, int nbQuestions, int m, int n, int nbRuns, String title)
			throws IOException {
		final ImmutableList.Builder<Run> builder = ImmutableList.builder();
		Strategy st = strategy;
		for (int i = 0; i < nbRuns; ++i) {
			if (strategy.getStrategyType().equals(StrategyType.TWO_PHASES_HEURISTIC)) {
				st = StrategyTwoPhasesHeuristic.build(qV, qC, committeeFirst);
			}
			final Run run = run(st, m, n, nbQuestions);
			builder.add(run);
			System.out.println("Run " + (i + 1) + " of " + nbRuns);
			System.out.println("mean avg time: " + run.getAvgQuestionTimesMs() + " ms");
		}
		final Runs runs = Runs.of(builder.build());
		printQuestions(runs, title, nbQuestions);
		return runs;
	}

	private static void printQuestions(Runs runs, String title, int nbQuestions) throws IOException {
		int nbRuns = runs.nbRuns();

		final CsvWriter qstWriter = new CsvWriter(Files.newOutputStream(Path.of(title + "_questions.csv")),
				new CsvWriterSettings());
		LinkedList<String> head = new LinkedList<>();
		for (int j = 0; j < nbRuns; j++) {
			head.add("qV(1)/qC(0)");
			head.add("Run " + (j + 1) + " of " + nbRuns);
		}
		final ImmutableList<String> qstHeaders = Stream.concat(Stream.of("i"), head.stream())
				.collect(ImmutableList.toImmutableList());
		qstWriter.writeHeaders(qstHeaders);

		for (int i = 0; i < nbQuestions; i++) {
			qstWriter.addValue(i + 1);
			for (int col = 0; col < nbRuns; col++) {
				final ImmutableList<Question> questions = runs.getRun(col).getQuestions();
				final Question q = questions.get(i);
				if (q.getType().equals(QuestionType.COMMITTEE_QUESTION)) {
					qstWriter.addValue(0);
				} else {
					qstWriter.addValue(1);
				}
				qstWriter.addValue(q);
			}
			qstWriter.writeValuesToRow();
		}
		qstWriter.close();
		return;
	}

	public static Run run(Strategy strategy, int m, int n, int k) {
		final Oracle oracle = Oracle.build(ImmutableMap.copyOf(Generator.genProfile(m, n)), Generator.genWeights(m));

		final PrefKnowledge knowledge = PrefKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
		strategy.setKnowledge(knowledge);

		final ImmutableList.Builder<Question> qBuilder = ImmutableList.builder();
		final ImmutableList.Builder<Long> tBuilder = ImmutableList.builder();

		for (int i = 1; i <= k; i++) {
			final long startTime = System.currentTimeMillis();
			final Question q = strategy.nextQuestion();
			final Answer a = oracle.getAnswer(q);
			knowledge.update(q, a);
//			LOGGER.info("Asked {}.", q);
			qBuilder.add(q);
			tBuilder.add(startTime);
		}

		final long endTime = System.currentTimeMillis();
		return Run.of(oracle, tBuilder.build(), qBuilder.build(), endTime);
	}

}

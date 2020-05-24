package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.Stats;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.strategies.Strategy;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class VariousXps {

	private static final String root = Paths.get("").toAbsolutePath() + "/experiments/";
	private static int qV = 0;
	private static int qC = 0;
	private static boolean committeeFirst = true;

	public static void main(String[] args) throws IOException {
		final int k = 500; // nbQuestions
		final int nbRuns = 10;
		final int m = 10; // alternatives
		final int n = 20; // agents
		String head = "nbQst = " + k;

		// Strategy stRandom = StrategyRandom.build();
		// runXP(k, nbRuns, m, n, stRandom, head);

		// Strategy stPessimistic = StrategyPessimistic.build(AggOps.MAX);
		// runXP(k, nbRuns, m, n, stPessimistic, head);

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

		// Strategy stTwoPhHeuristic;

		// for (int i = 0; i <= k; i += 50) {
		// qV = i;
		// qC = k - i;
		// System.out.println(qV);
		// stTwoPhHeuristic = StrategyTwoPhasesHeuristic.build(qV, qC, committeeFirst);
		// head = "qC = " + qC + " then qV = " + qV;
		// try {
		// runXP(k, nbRuns, m, n, stTwoPhHeuristic, head);
		// } catch (IllegalStateException e) {
		// System.out.println("Complete at qV " + qV);
		// e.printStackTrace();
		// break;
		// }
		// }

		committeeFirst = false;
		for (int i = 250; i >= 0; i -= 50) {
			qV = i;
			qC = k - i;
			System.out.println(qV);
			head = "qC = " + qC + " then qV = " + qV;
			try {
				runXP(k, nbRuns, m, n, StrategyFactory.twoPhases(qV, qC, committeeFirst), head);
			} catch (IllegalStateException e) {
				System.out.println("Complete at qV " + qV);
				e.printStackTrace();
				break;
			}
		}

	}

	private static void runXP(int k, int nbRuns, int m, int n, Supplier<Strategy> strategyFactory, String head)
			throws IOException {
		String title = root + "m" + m + "n" + n + strategyFactory.toString() + "_" + k;
		final Runs runs = runRepeatedly(strategyFactory, k, m, n, nbRuns, title);

		System.out.println("Time for the stats:");
		long s = System.currentTimeMillis();
		final ImmutableList<Double> regrets = runs.getAverageMinimalMaxRegrets();
		System.out.println((System.currentTimeMillis() - s) / 1000);

		final ImmutableMap.Builder<String, ImmutableList<Double>> builder = ImmutableMap.builder();
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

	private static Runs runRepeatedly(Supplier<Strategy> strategyFactory, int nbQuestions, int m, int n, int nbRuns,
			String title) throws IOException {
		final ImmutableList.Builder<Run> builder = ImmutableList.builder();
		for (int i = 0; i < nbRuns; ++i) {
			final Strategy st = strategyFactory.get();
			final Run run = Runner.run(st, m, n, nbQuestions);
			builder.add(run);
			System.out.println("Run " + (i + 1) + " of " + nbRuns);
			System.out.println("mean avg time: " + Stats.of(run.getQuestionTimesMs()).mean() + " ms");
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

}

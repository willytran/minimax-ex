package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.math.Stats;

import io.github.oliviercailloux.json.PrintableJsonObject;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.PreferenceInformation;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.QuestioningConstraint;
import io.github.oliviercailloux.minimax.strategies.StrategyByMmr;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.strategies.StrategyHelper;
import io.github.oliviercailloux.minimax.utils.Generator;

public class VariousXps {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(VariousXps.class);

	public static void main(String[] args) throws Exception {
		final VariousXps variousXps = new VariousXps();
//		variousXps.exportOracles(30, 5, 100);
//		variousXps.tiesWithOracle1();
		variousXps.runWithOracle1();
//		variousXps.analyzeQuestions();
	}

	public void runWithOracle1() throws IOException {
		final int m = 6;
		final int n = 6;
		final int k = 30;
		final ThreadLocalRandom random = ThreadLocalRandom.current();
		final ImmutableList.Builder<StrategyFactory> factoriesBuilder = ImmutableList.<StrategyFactory>builder();
		factoriesBuilder.add(StrategyFactory.limited(random.nextLong(), ImmutableList.of()));
		for (int qC = 2; qC < k; qC += 2) {
			final int qV = k - qC;
			factoriesBuilder.add(StrategyFactory.limited(random.nextLong(),
					ImmutableList.of(QuestioningConstraint.of(QuestionType.COMMITTEE_QUESTION, qC),
							QuestioningConstraint.of(QuestionType.VOTER_QUESTION, qV))));
			factoriesBuilder.add(StrategyFactory.limited(random.nextLong(),
					ImmutableList.of(QuestioningConstraint.of(QuestionType.VOTER_QUESTION, qV),
							QuestioningConstraint.of(QuestionType.COMMITTEE_QUESTION, qC))));
		}

		final Path json = Path.of("experiments/Oracles/", String.format("Oracles m = %d, n = %d, 100.json", m, n));
		final List<Oracle> oracles = JsonConverter.toOracles(Files.readString(json));
		final Oracle oracle = oracles.get(0);

		for (StrategyFactory factory : factoriesBuilder.build()) {
			final Runs runs = runs(factory, oracle, k, 50);
			final Stats stats = runs.getMinimalMaxRegretStats().get(runs.getK());
			final String descr = Runner.asStringEstimator(stats);
			LOGGER.info("Got final estimator: {}.", descr);
		}
	}

	public void tiesWithOracle1() throws IOException {
		final int m = 5;
		final int n = 5;
		final int k = 20;
		final long seed = ThreadLocalRandom.current().nextLong();
		final StrategyFactory factory = StrategyFactory.limited(seed, ImmutableList.of());
		// final StrategyFactory factory = StrategyFactory.limited(seed,
		// ImmutableList.of(QuestioningConstraint.of(QuestionType.VOTER_QUESTION,
		// Integer.MAX_VALUE)));
		// final StrategyFactory factory = StrategyFactory.byMmrs(seed,
		// MmrLottery.MAX_COMPARATOR);
		// final StrategyFactory factory = StrategyFactory.random();

		final Path json = Path.of("experiments/Oracles/", String.format("Oracles m = %d, n = %d, 100.json", m, n));
		final List<Oracle> oracles = JsonConverter.toOracles(Files.readString(json));
		final Oracle oracle = oracles.get(0);

		for (int i = 0; i < 5; ++i) {
			runShowTies((StrategyByMmr) factory.get(), oracle, k);
		}
	}

	public void exportOracles(int m, int n, int count) throws IOException {
		final ImmutableList.Builder<Oracle> builder = ImmutableList.<Oracle>builder();
		for (int i = 0; i < count; ++i) {
			final Oracle oracle = Oracle.build(Generator.genProfile(m, n), Generator.genWeightsEquallySpread(m));
			builder.add(oracle);
		}
		final ImmutableList<Oracle> oracles = builder.build();
		final PrintableJsonObject json = JsonConverter.toJson(oracles);
		Files.writeString(
				Path.of("experiments/Oracles/", String.format("Oracles m = %d, n = %d, %d.json", m, n, count)),
				json.toString());
	}

	public Runs runs(StrategyFactory factory, Oracle oracle, int k, int nbRuns) throws IOException {
		final Path outDir = Path.of("experiments/");
		Files.createDirectories(outDir);
		final String prefixDescription = factory.getDescription() + ", m = " + oracle.getM() + ", n = " + oracle.getN()
				+ ", k = " + k;
		final String prefixTemp = prefixDescription + ", ongoing";
		final Path tmpJson = outDir.resolve(prefixTemp + ".json");
		final Path tmpCsv = outDir.resolve(prefixTemp + ".csv");

		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		LOGGER.info("Started '{}'.", factory.getDescription());
		for (int i = 0; i < nbRuns; ++i) {
			final Run run = Runner.run(factory, oracle, k);
			LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
			runsBuilder.add(run);
			final Runs runs = Runs.of(factory, runsBuilder.build());
			// Runner.summarize(runs);
			Files.writeString(tmpJson, JsonConverter.toJson(runs).toString());
			Files.writeString(tmpCsv, ToCsv.toCsv(runs, 1));
		}

		final String prefix = prefixDescription + ", nbRuns = " + nbRuns;
		final Path outJson = outDir.resolve(prefix + ".json");
		final Path outCsv = outDir.resolve(prefix + ".csv");
		Files.move(tmpJson, outJson, StandardCopyOption.REPLACE_EXISTING);
		Files.move(tmpCsv, outCsv, StandardCopyOption.REPLACE_EXISTING);

		return Runs.of(factory, runsBuilder.build());
	}

	public void runShowTies(StrategyByMmr strategy, Oracle oracle, int k) {
		LOGGER.info("Running with {}, {}.", oracle, k);
		final PrefKnowledge knowledge = PrefKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
		strategy.setKnowledge(knowledge);

		final ImmutableList.Builder<Question> qBuilder = ImmutableList.builder();
		final ImmutableList.Builder<Integer> tiesBuilder = ImmutableList.builder();

		for (int i = 1; i <= k; i++) {
			final Question q = strategy.nextQuestion();
			final PreferenceInformation a = oracle.getPreferenceInformation(q);
			knowledge.update(a);
			LOGGER.debug("Asked {}.", q);
			qBuilder.add(q);
			final int countTies = StrategyHelper
					.getMinimalElements(strategy.getLastQuestions(), strategy.getLotteryComparator()).size();
			tiesBuilder.add(countTies);
		}

		final ImmutableList<Question> questions = qBuilder.build();
		final ImmutableList<Integer> ties = tiesBuilder.build();
		LOGGER.debug("Questions:{}.", questions);
		LOGGER.info("Ties: {}.", ties);
	}

	public void analyzeQuestions() throws Exception {
		final int m = 7;
		final int n = 7;
		final int k = 30;
		final int nbRuns = 50;
		final Path json = Path.of(String.format(
				"experiments/Limited, constrained to [], m = %d, n = %d, k = %d, nbRuns = %d.json", m, n, k, nbRuns));
		final Runs runs = JsonConverter.toRuns(Files.readString(json));
		for (Run run : runs.getRuns()) {
			LOGGER.info("Run: {} qC, {} qV, mmr {}.", run.getNbQCommittee(), run.getNbQVoters(),
					run.getMinimalMaxRegrets().get(k).getMinimalMaxRegretValue());
		}
	}
}

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
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.utils.Generator;

public class VariousXps {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(VariousXps.class);

	public static void main(String[] args) throws Exception {
		final VariousXps variousXps = new VariousXps();
//		variousXps.exportOracles(5, 5, 100);
		variousXps.runWithOracle1();
	}

	public Runs runWithOracle1() throws IOException {
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

		final Runs runs = runs(factory, oracles.get(0), k, 50);
		final Stats stats = runs.getMinimalMaxRegretStats().get(runs.getMaxK());
		final String descr = Runner.asStringEstimator(stats);
		LOGGER.info("Got final estimator: {}.", descr);

		return runs;
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
}

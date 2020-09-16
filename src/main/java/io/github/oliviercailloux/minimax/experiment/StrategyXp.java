package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.QuestioningConstraint;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.utils.Generator;

public class StrategyXp {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyXp.class);

	public static void main(String[] args) throws Exception {
		final int m = 5;
		final int n = 10;
		final int k = 150;
		final long seed = ThreadLocalRandom.current().nextLong();
//		final StrategyFactory factory = StrategyFactory.limited();
//		final StrategyFactory factory = StrategyFactory.limited(seed,
//				ImmutableList.of(QuestioningConstraint.of(QuestionType.VOTER_QUESTION, Integer.MAX_VALUE)));

//		for(int i=150; i<=k; i+=50) {
//			final StrategyFactory factoryVotFirst = StrategyFactory.limitedVotersThenCommittee(i);
//			final StrategyFactory factoryComFirst = StrategyFactory.limitedCommitteeThenVoters(i);
//			runs(factoryVotFirst, m, n, k, 5);
//			runs(factoryComFirst, m, n, k, 5);
//		}
//		
//		final StrategyFactory factoryVotFirst = StrategyFactory.limitedVotersThenCommittee(k-25);
//		final StrategyFactory factoryComFirst = StrategyFactory.limitedCommitteeThenVoters(25);
//		runs(factoryVotFirst, m, n, k, 5);
//		runs(factoryComFirst, m, n, k, 5);

//		final StrategyFactory factory = StrategyFactory.random();
		final ImmutableList<StrategyFactory> factoryList = ImmutableList.of(StrategyFactory.random(),
				StrategyFactory.pessimistic(), StrategyFactory.limited());
		runs(factoryList, m, n, k, 25);

	}

	/**
	 * Repeat (nbRuns times) a run experiment (thus: generate an oracle, ask k
	 * questions).
	 */
	public static void runs(ImmutableList<StrategyFactory> factoryList, int m, int n, int k, int nbRuns)
			throws IOException {
		final Path outDir = Path.of("experiments/Strategy/");
		Files.createDirectories(outDir);
		final ImmutableMap.Builder<StrategyFactory, Path> tmpJsonMapBuilder = ImmutableMap.builder();
		final ImmutableMap.Builder<StrategyFactory, Path> tmpCsvMapBuilder = ImmutableMap.builder();
		for (StrategyFactory factory : factoryList) {
			final String prefixTemp = factory.getDescription() + ", m = " + m + ", n = " + n + ", k = " + k
					+ ", ongoing";
			tmpJsonMapBuilder.put(factory, outDir.resolve(prefixTemp + ".json"));
			tmpCsvMapBuilder.put(factory, outDir.resolve(prefixTemp + ".csv"));
		}
		final ImmutableMap<StrategyFactory, Path> tmpJsonMap = tmpJsonMapBuilder.build();
		final ImmutableMap<StrategyFactory, Path> tmpCsvMap = tmpCsvMapBuilder.build();

		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		// LOGGER.info("Started '{}'.", factory.getDescription());
		for (int i = 0; i < nbRuns; ++i) {
			final Oracle oracle = Oracle.build(Generator.genProfile(m, n), Generator.genWeights(m));
			for (StrategyFactory factory : factoryList) {
				final Run run = Runner.run(factory, oracle, k);
				LOGGER.info("Strategy {} - Time (run {}): {}.", factory.getDescription(), i, run.getTotalTime());
				Files.writeString(Path.of("run " + i + ".json"), JsonConverter.toJson(run).toString());
				runsBuilder.add(run);
				final Runs runs = Runs.of(factory, runsBuilder.build());
//				Runner.summarize(runs);
				Files.writeString(tmpJsonMap.get(factory), JsonConverter.toJson(runs).toString());
				Files.writeString(tmpCsvMap.get(factory), ToCsv.toCsv(runs));
			}
		}

		for (StrategyFactory factory : factoryList) {
			final String prefix = factory.getDescription() + ", m = " + m + ", n = " + n + ", k = " + k + ", nbRuns = "
					+ nbRuns;
			final Path outJson = outDir.resolve(prefix + ".json");
			final Path outCsv = outDir.resolve(prefix + ".csv");
			Files.move(tmpJsonMap.get(factory), outJson, StandardCopyOption.REPLACE_EXISTING);
			Files.move(tmpCsvMap.get(factory), outCsv, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}

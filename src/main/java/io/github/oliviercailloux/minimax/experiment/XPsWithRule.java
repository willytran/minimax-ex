package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apfloat.Aprational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.math.Stats;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class XPsWithRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableLinearityXps.class);

    public static void main(String[] args) throws Exception {
	final XPsWithRule xps = new XPsWithRule();
	xps.runWithRule("Borda", 10, 20, 10, 1);
    }

    public void runWithRule(String rule, int m, int n, int k, int nbRuns) throws IOException {
	StrategyFactory factory = StrategyFactory.limited();

	final Path json = Path.of("experiments/Oracles/",
		String.format("Borda, m = %d, n = %d, %d.json", m, n, nbRuns));
	final ImmutableList<Oracle> oracles = ImmutableList.copyOf(JsonConverter.toOracles(Files.readString(json)));
	final String prefixDescription = rule + ", m = " + m + ", n = " + n + ", k = " + k;

	final Path outDir = Path.of("experiments/TableLinearity");
	Files.createDirectories(outDir);
	final String prefixTemp = prefixDescription + ", ongoing";
	final Path tmpJson = outDir.resolve(prefixTemp + ".json");
	final Path tmpCsv = outDir.resolve(prefixTemp + ".csv");

	final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
	LOGGER.info("Started '{}'.", factory.getDescription());
	for (int i = 0; i < nbRuns; ++i) {
	    final Oracle oracle = oracles.get(i);
	    LOGGER.info("Before run.");

	    final UpdateablePreferenceKnowledge knowledge = UpdateablePreferenceKnowledge
		    .given(oracle.getAlternatives(), oracle.getProfile().keySet());
	    System.out.println(knowledge.getLambdaRange(1));
	    ImmutableList<Alternative> al = oracle.getAlternatives().asList();
	    for (int j = 1; j <= m - 2; j++) {
		Aprational a = new Aprational(oracle.getScore(al.get(j - 1)) - oracle.getScore(al.get(j)));
		Aprational b = new Aprational(oracle.getScore(al.get(j)) - oracle.getScore(al.get(j + 1)));
		Aprational lambda = a.divide(b);
		System.out.println(j + " " + lambda.doubleValue());
		knowledge.addConstraint(j, ComparisonOperator.EQ, lambda);
	    }

	    final Run run = Runner.run(factory.get(), oracle, knowledge, k);
	    LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
	    runsBuilder.add(run);
	    final Runs runs = Runs.of(factory, runsBuilder.build());
	    LOGGER.info("Runs.");
	    // Runner.summarize(runs);
	    Files.writeString(tmpJson, JsonConverter.toJson(runs).toString());
	    LOGGER.info("Written json.");
	    Files.writeString(tmpCsv, ToCsv.toCsv(runs, 1));
	    LOGGER.info("Written csv.");
	}

	final String prefix = prefixDescription + ", nbRuns = " + nbRuns;
	final Path outJson = outDir.resolve(prefix + ".json");
	final Path outCsv = outDir.resolve(prefix + ".csv");
	Files.move(tmpJson, outJson, StandardCopyOption.REPLACE_EXISTING);
	Files.move(tmpCsv, outCsv, StandardCopyOption.REPLACE_EXISTING);

	final Runs runs = Runs.of(factory, runsBuilder.build());

	final Stats stats = runs.getMinimalMaxRegretStats().get(runs.getK());
	final String descr = Runner.asStringEstimator(stats);
	LOGGER.info("Got final estimator: {}.", descr);
    }

}

package io.github.oliviercailloux.minimax.experiment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;

public class AnalyzeJsons {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeJsons.class);

    public static void main(String[] args) throws Exception {
	findAndAnalyze();
    }

    public static void findAndAnalyze() throws Exception {
	final Path inDir = Path.of("experiments/Small/");
	final ImmutableSet<Path> jsonPaths;
	try (Stream<Path> paths = Files.list(inDir)) {
	    jsonPaths = paths.filter(p -> p.getFileName().toString().endsWith(".json"))
		    .collect(ImmutableSet.toImmutableSet());
	}
	for (Path json : jsonPaths) {
	    analyze(json);
	}
    }

    public static void analyze(Path json) throws Exception {
	final Runs runs = JsonConverter.toRuns(Files.readString(json));
	int i = 0;
	for (Run run : runs.getRuns()) {
	    final double value = run.getMinimalMaxRegrets().get(0).getMinimalMaxRegretValue();
	    final int n = run.getOracle().getN();
	    LOGGER.info("i: {}, value: {}, n: {}.", i, value, n);
//			if (value == 3.0) {
//				Files.writeString(Path.of("run.json"), JsonConverter.toJson(run).toString());
//				break;
//			}
//			verify(value == m, String.format("Value: %s, m: %s.", value, m));
	    ++i;
	}
    }
}

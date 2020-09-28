package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.json.PrintableJsonObject;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.utils.Generator;

public class VariousXps {
	public static void main(String[] args) throws Exception {
		new VariousXps().exportOracles(5, 5, 100);
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
}

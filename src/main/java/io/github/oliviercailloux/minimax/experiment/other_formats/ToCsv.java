package io.github.oliviercailloux.minimax.experiment.other_formats;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.Stats;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

import io.github.oliviercailloux.minimax.experiment.Runs;

public class ToCsv {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(ToCsv.class);

	public static String toCsv(Runs runs, int modulo) {
		checkArgument(modulo >= 1);
		final ImmutableMap<Integer, Stats> everyFive = IntStream.rangeClosed(0, runs.getK())
				.filter(i -> i % modulo == 0).boxed()
				.collect(ImmutableMap.toImmutableMap(i -> i, i -> runs.getMinimalMaxRegretStats().get(i)));

		final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
		formatter.setMaximumFractionDigits(2);

		final StringWriter stringWriter = new StringWriter();
		final CsvWriter writer = new CsvWriter(stringWriter, new CsvWriterSettings());
		writer.writeHeaders("k", "MMR min", "MMR avg", "MMR max", "MMR σ");
		final ImmutableSet<Integer> ks = everyFive.keySet();
		for (Integer k : ks) {
			final Stats stat = everyFive.get(k);
			writer.addValue("k", k);
			writer.addValue("MMR min", formatter.format(stat.min()));
			writer.addValue("MMR avg", formatter.format(stat.mean()));
			writer.addValue("MMR max", formatter.format(stat.max()));
			writer.addValue("MMR σ", formatter.format(stat.populationStandardDeviation()));
			writer.writeValuesToRow();
		}
		return stringWriter.toString();
	}

}

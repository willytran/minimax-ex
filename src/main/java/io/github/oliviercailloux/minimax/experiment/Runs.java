package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTypeAdapter;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.google.common.math.Stats;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.json.RunsAdapter;

@JsonbTypeAdapter(RunsAdapter.class)
public class Runs {

	@JsonbCreator
	public static Runs of(@JsonbProperty("runs") List<Run> runs) {
		return new Runs(runs);
	}

	private final ImmutableList<Run> runs;
	private final int maxK;

	private Runs(List<Run> runs) {
		checkArgument(!runs.isEmpty());
		this.runs = ImmutableList.copyOf(runs);
		maxK = runs.stream().mapToInt(Run::getK).max().getAsInt();
		final ImmutableSet<Integer> ms = runs.stream().map(Run::getOracle).map(Oracle::getM)
				.collect(ImmutableSet.toImmutableSet());
		final ImmutableSet<Integer> ns = runs.stream().map(Run::getOracle).map(Oracle::getN)
				.collect(ImmutableSet.toImmutableSet());
		checkArgument(runs.stream().allMatch(
				r -> r.getK() < maxK ? r.getMinimalMaxRegrets().get(r.getK()).getMinimalMaxRegretValue() == 0d : true),
				"All runs should have either k questions or end with no regret.");
		checkArgument(ms.size() == 1, "All runs should have the same number of alternatives.");
		checkArgument(ns.size() == 1, "All runs should have the same number of voters.");
	}

	/**
	 * @return a list of size k + 1.
	 */
	public ImmutableList<Double> getAverageMinimalMaxRegrets() {
		final ImmutableList<Stats> stats = getMinimalMaxRegretStats();
		return stats.stream().map(Stats::mean).collect(ImmutableList.toImmutableList());
	}

	/**
	 * @return a list of size k + 1.
	 */
	public ImmutableList<Stats> getMinimalMaxRegretStats() {
		final ImmutableList.Builder<Stats> statsBuilder = ImmutableList.builder();
		for (int i = 0; i < maxK + 1; ++i) {
			final int finali = i;
			final ImmutableList<Double> allIthRegrets = runs.stream().map((r) -> r.getMinimalMaxRegrets())
					.map(l -> (finali < l.size() ? l.get(finali).getMinimalMaxRegretValue() : 0d))
					.collect(ImmutableList.toImmutableList());
			statsBuilder.add(Stats.of(allIthRegrets));
		}
		return statsBuilder.build();
	}

	public Stats getQuestionTimeStats() {
		final ImmutableList<Integer> allTimes = runs.stream().flatMap((r) -> r.getQuestionTimesMs().stream())
				.collect(ImmutableList.toImmutableList());
		return Stats.of(allTimes);
	}

	public Stats getTotalTimeStats() {
		final IntStream totalTimesStream = runs.stream().mapToInt(Run::getTotalTimeMs);
		return Stats.of(totalTimesStream);
	}

	public int nbRuns() {
		return runs.size();
	}

	public ImmutableList<Run> getRuns() {
		return runs;
	}

	public Run getRun(int i) {
		checkArgument(i < runs.size());
		return runs.get(i);
	}

	public int getMaxK() {
		return maxK;
	}

	public int getM() {
		return runs.stream().map(Run::getOracle).map(Oracle::getM).collect(MoreCollectors.onlyElement());
	}

	public int getN() {
		return runs.stream().map(Run::getOracle).map(Oracle::getN).collect(MoreCollectors.onlyElement());
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof Runs)) {
			return false;
		}

		final Runs r2 = (Runs) o2;
		return runs.equals(r2.runs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(runs);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("runs", runs).toString();
	}
}

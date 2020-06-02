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
import com.google.common.math.Stats;

import io.github.oliviercailloux.minimax.experiment.json.RunsAdapter;
import io.github.oliviercailloux.minimax.regret.Regrets;

@JsonbTypeAdapter(RunsAdapter.class)
public class Runs {

	@JsonbCreator
	public static Runs of(@JsonbProperty("runs") List<Run> runs) {
		return new Runs(runs);
	}

	private final ImmutableList<Run> runs;
	private final int k;

	private Runs(List<Run> runs) {
		checkArgument(!runs.isEmpty());
		this.runs = ImmutableList.copyOf(runs);
		final ImmutableSet<Integer> ks = runs.stream().map(Run::getK).collect(ImmutableSet.toImmutableSet());
		checkArgument(ks.size() == 1, "All runs should have the same number of questions.");
		k = ks.iterator().next();
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
		for (int i = 0; i < k + 1; ++i) {
			final int finali = i;
			final ImmutableList<Double> allIthRegrets = runs.stream().map((r) -> r.getMinimalMaxRegrets().get(finali))
					.map(Regrets::getMinimalMaxRegretValue).collect(ImmutableList.toImmutableList());
			statsBuilder.add(Stats.of(allIthRegrets));
		}
		return statsBuilder.build();
	}

	/**
	 * @return a list of size k.
	 */
	public ImmutableList<Stats> getQuestionTimeStats() {
		final ImmutableList.Builder<Stats> statsBuilder = ImmutableList.builder();
		for (int i = 0; i < k; ++i) {
			final int finali = i;
			final ImmutableList<Integer> allIths = runs.stream().map((r) -> r.getQuestionTimesMs().get(finali))
					.collect(ImmutableList.toImmutableList());
			statsBuilder.add(Stats.of(allIths));
		}
		return statsBuilder.build();
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

	public int getK() {
		return k;
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

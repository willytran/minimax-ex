package io.github.oliviercailloux.experiment;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.Stats;

import io.github.oliviercailloux.minimax.regret.Regrets;

public class Runs {

	private ImmutableList<Run> runs;
	private final int k;

	public static Runs of(List<Run> runs) {
		return new Runs(runs);
	}

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
		final ImmutableList.Builder<Double> average = ImmutableList.builder();
		for (int i = 0; i < k + 1; ++i) {
			final int finali = i;
			final ImmutableList<Double> allIthRegrets = runs.stream().map((r) -> r.getMinimalMaxRegrets().get(finali))
					.map(Regrets::getMinimalMaxRegretValue).collect(ImmutableList.toImmutableList());
			final Stats stats = Stats.of(allIthRegrets);
			average.add(stats.mean());
		}
		return average.build();
	}

	public int nbRuns() {
		return runs.size();
	}

	public Run getRun(int i) {
		checkArgument(i < runs.size());
		return runs.get(i);
	}
}

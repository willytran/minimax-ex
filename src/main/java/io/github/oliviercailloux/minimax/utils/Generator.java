package io.github.oliviercailloux.minimax.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;

public class Generator {

	public static PSRWeights genWeightsEquallySpread(int nbAlternatives) {
		final Random r = new Random();
		final Supplier<Double> differenceSupplier = () -> r.nextDouble();

		return genWeights(nbAlternatives, differenceSupplier);
	}

	public static PSRWeights genWeights(int nbAlternatives) {
		final Random r = new Random();
		final double p = (1 / (double) (nbAlternatives - 1));
		final Supplier<Double> differenceSupplier = () -> r.nextDouble() < p ? 0.9 + (r.nextDouble() * 0.1)
				: (r.nextDouble() * 0.1);

		return genWeights(nbAlternatives, differenceSupplier);
	}

	private static PSRWeights genWeights(int nbAlternatives, Supplier<Double> differenceSupplier) {
		final ImmutableList.Builder<Double> weightsBuilder = ImmutableList.<Double>builder();
		if (nbAlternatives == 0) {
			return PSRWeights.given(weightsBuilder.build());
		}

		weightsBuilder.add(1d);
		if (nbAlternatives == 1) {
			return PSRWeights.given(weightsBuilder.build());
		}

		final ImmutableList<Double> differences = IntStream.range(0, nbAlternatives - 1)
				.mapToObj(i -> differenceSupplier.get()).collect(ImmutableList.toImmutableList());

		final double sum = differences.stream().mapToDouble(d -> d).sum();
		verify(sum > 0d);
		final List<Double> normalizedDifferences = differences.stream().map(d -> d / sum)
				.collect(Collectors.toCollection(ArrayList::new));

		Collections.sort(normalizedDifferences, Comparator.reverseOrder());

		double previous = 1d;
		for (double difference : normalizedDifferences.subList(0, normalizedDifferences.size() - 1)) {
			double curr = (previous - difference);
			if (previous < difference) {
				/**
				 * Because of imprecision when normalizing, we could end up negative. In this
				 * case, we check that this is only the result of a small imprecision error (not
				 * some bigger logical error), and round to zero.
				 */
				verify(curr > -1e10);
				weightsBuilder.add(0d);
			} else {
				weightsBuilder.add(curr);
			}
			previous = curr;
		}

		final Double lastDifference = normalizedDifferences.get(normalizedDifferences.size() - 1);
		verify(DoubleMath.fuzzyEquals(previous, lastDifference, 1e10));
		weightsBuilder.add(0d);
		return PSRWeights.given(weightsBuilder.build());
	}

	public static Map<Voter, VoterStrictPreference> genProfile(int nbAlternatives, int nbVoters) {
		checkArgument(nbVoters >= 1);
		checkArgument(nbAlternatives >= 1);
		Map<Voter, VoterStrictPreference> profile = new HashMap<>();

		List<Alternative> availableRanks = new LinkedList<>();
		for (int i = 1; i <= nbAlternatives; i++) {
			availableRanks.add(Alternative.withId(i));
		}

		for (int i = 1; i <= nbVoters; ++i) {
			Voter v = Voter.withId(i);
			List<Alternative> linearOrder = Lists.newArrayList(availableRanks);
			Collections.shuffle(linearOrder);
			VoterStrictPreference pref = VoterStrictPreference.given(v, linearOrder);
			profile.put(v, pref);
		}

		verify(profile.size() == nbVoters);
		return profile;
	}

	public static Oracle generateOracle(int nbAlternatives, int nbVoters) {
		return Oracle.build(genProfile(nbAlternatives, nbVoters), genWeights(nbAlternatives));
	}
}

package io.github.oliviercailloux.minimax.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;

public class Generator {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

	public static PSRWeights genWeights(int nbAlternatives) {
		List<Double> weights = new LinkedList<>();
		weights.add(1d);
		Random r = new Random();
		double[] differences = new double[nbAlternatives - 1];
		double sum = 0;
		double p = (1 / (double) (nbAlternatives - 1));
		for (int i = 1; i < nbAlternatives - 1; i++) {
			differences[i] = r.nextDouble() < p ? 0.9 + (r.nextDouble() * 0.1) : (r.nextDouble() * 0.1);
			sum += differences[i];
		}
		for (int i = 0; i < nbAlternatives - 1; i++) {
			differences[i] = differences[i] / sum;
		}
		Arrays.sort(differences);
		double previous = 1d;
		for (int i = nbAlternatives - 2; i > 0; i--) {
			final double difference = differences[i];
			double curr = (previous - difference);
			if (previous < difference) {
				/**
				 * Because of imprecision when normalizing, we could end up in the negative. In
				 * this case, we check that this is only the result of a small imprecision error
				 * (not some bigger logical error), and round to zero.
				 */
				verify(curr > -1e10);
				weights.add(0d);
			} else {
				weights.add(curr);
			}
			previous = curr;
		}
		weights.add(0d);
		return PSRWeights.given(weights);
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

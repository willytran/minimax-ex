package io.github.oliviercailloux.minimax.experiment.json;

import java.util.List;

import javax.json.bind.adapter.JsonbAdapter;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.StrictPreference;

public class StrictPreferenceAdapter implements JsonbAdapter<StrictPreference, List<Integer>> {
	@Override
	public List<Integer> adaptToJson(StrictPreference preference) {
		final ImmutableList<Integer> orderedAlts = preference.getAlternatives().stream().map(Alternative::getId)
				.collect(ImmutableList.toImmutableList());
		return orderedAlts;
	}

	@Override
	public StrictPreference adaptFromJson(List<Integer> obj) {
		throw new UnsupportedOperationException();
	}
}

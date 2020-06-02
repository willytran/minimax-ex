package io.github.oliviercailloux.minimax.experiment.json;

import java.util.List;

import javax.json.bind.adapter.JsonbAdapter;

import io.github.oliviercailloux.minimax.experiment.Run;
import io.github.oliviercailloux.minimax.experiment.Runs;

public class RunsAdapter implements JsonbAdapter<Runs, List<Run>> {
	@Override
	public List<Run> adaptToJson(Runs obj) {
		return obj.getRuns();
	}

	@Override
	public Runs adaptFromJson(List<Run> obj) {
		throw new UnsupportedOperationException();
	}
}

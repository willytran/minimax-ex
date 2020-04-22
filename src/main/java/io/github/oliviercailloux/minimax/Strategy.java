package io.github.oliviercailloux.minimax;

import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;

public interface Strategy {

	public void setKnowledge(PrefKnowledge knowledge);

	/**
	 * Returns the next question that this strategy thinks is best asking.
	 *
	 * @return a question.
	 * @throws IllegalArgumenteException if there are less than two alternatives.
	 */
	public Question nextQuestion() throws IllegalStateException;
}

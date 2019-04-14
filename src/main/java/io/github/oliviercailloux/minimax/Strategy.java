package io.github.oliviercailloux.minimax;

import io.github.oliviercailloux.minimax.elicitation.Question;

public interface Strategy {
	/**
	 * Returns the next question that this strategy thinks is best asking.
	 *
	 * @return a question.
	 * @throws IllegalStateException if everything is known.
	 */
	public Question nextQuestion() throws IllegalStateException;
}

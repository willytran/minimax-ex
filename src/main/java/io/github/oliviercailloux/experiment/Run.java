package io.github.oliviercailloux.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.minimax.regret.Regrets;

public class Run {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Run.class);

	public static Run of(Oracle oracle, List<Long> startTimes, List<Question> questions, long endTime) {
		return new Run(oracle, startTimes, questions, endTime);
	}

	private final Oracle oracle;
	private final ImmutableList<Long> startTimes;
	private final ImmutableList<Question> questions;
	private final long endTime;
	private ImmutableList<Regrets> regrets;

	private Run(Oracle oracle, List<Long> startTimes, List<Question> questions, long endTime) {
		checkArgument(!startTimes.isEmpty());
		checkArgument(startTimes.size() == questions.size());
		checkArgument(questions.size() >= 1);
		checkArgument(endTime >= startTimes.get(startTimes.size() - 1));
		this.oracle = oracle;
		this.startTimes = ImmutableList.copyOf(startTimes);
		this.questions = ImmutableList.copyOf(questions);
		this.endTime = endTime;
		this.regrets = null;
		verify((getNbQVoters() + getNbQCommittee()) == questions.size());
	}

	public Oracle getOracle() {
		return oracle;
	}

	public int getK() {
		return questions.size();
	}

	/**
	 * @return a list of size k.
	 */
	public ImmutableList<Question> getQuestions() {
		return questions;
	}

	/**
	 * @return a list of size k.
	 */
	public ImmutableList<Long> getQuestionTimesMs() {
		final ImmutableList.Builder<Long> builder = ImmutableList.builder();

		final PeekingIterator<Long> peekingIterator = Iterators.peekingIterator(startTimes.iterator());
		while (peekingIterator.hasNext()) {
			final long start = peekingIterator.next();
			final long end;
			if (peekingIterator.hasNext()) {
				end = peekingIterator.peek();
			} else {
				end = endTime;
			}
			checkState(end - start >= 0l);
			builder.add(end - start);
		}

		final ImmutableList<Long> times = builder.build();
		verify(times.stream().mapToLong(Long::longValue).sum() == getTotalTimeMs());
		return times;
	}

	public int getNbQVoters() {
		return (int) questions.stream().filter((q) -> q.getType().equals(QuestionType.VOTER_QUESTION)).count();
	}

	public int getNbQCommittee() {
		return (int) questions.stream().filter((q) -> q.getType().equals(QuestionType.COMMITTEE_QUESTION)).count();
	}

	public double getPropQVoters() {
		return ((double) getNbQVoters()) / (double) (getNbQVoters() + getNbQCommittee());
	}

	public long getTotalTimeMs() {
		return endTime - startTimes.get(0);
	}

	/**
	 * @return a list of size k + 1.
	 */
	public ImmutableList<Regrets> getMinimalMaxRegrets() {
		if (regrets == null) {
			final PrefKnowledge knowledge = PrefKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
			final RegretComputer rc = new RegretComputer(knowledge);

			final ImmutableList.Builder<Regrets> builder = ImmutableList.builderWithExpectedSize(questions.size() + 1);
			builder.add(rc.getMinimalMaxRegrets());

			for (Question question : questions) {
				knowledge.update(question, oracle.getAnswer(question));
				builder.add(rc.getMinimalMaxRegrets());
			}

			regrets = builder.build();
		}
		return regrets;
	}
}

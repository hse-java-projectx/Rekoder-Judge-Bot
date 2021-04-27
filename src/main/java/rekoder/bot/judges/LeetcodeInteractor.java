package rekoder.bot.judges;

import rekoder.primitive.Problem;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

public class LeetcodeInteractor extends JudgeInteractor {
    public LeetcodeInteractor(Logger logger) {
        super(logger, "Leetcode");
    }

    @Override
    public List<Problem> getAllProblems() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Problem getProblemByUrl(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getProblemUrlsInInterval(LocalDateTime begin, LocalDateTime end) {
        throw new UnsupportedOperationException();
    }
}

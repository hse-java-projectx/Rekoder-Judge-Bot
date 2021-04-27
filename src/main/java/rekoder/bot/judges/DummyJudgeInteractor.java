package rekoder.bot.judges;

import rekoder.primitive.Problem;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

public class DummyJudgeInteractor extends JudgeInteractor {
    public DummyJudgeInteractor(Logger logger) {
        super(logger, "Dummy");
    }

    @Override
    public List<Problem> getAllProblems() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Problem getProblemByUrl(String url) {
        return new Problem(url, "Statement dummy placeholder");
    }

    @Override
    public List<String> getProblemUrlsInInterval(LocalDateTime begin, LocalDateTime end) {
        return List.of("url1", "url2", "url3", "url4");
    }
}

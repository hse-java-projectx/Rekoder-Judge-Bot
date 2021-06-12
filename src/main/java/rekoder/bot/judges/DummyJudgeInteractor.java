package rekoder.bot.judges;

import rekoder.primitive.Problem;

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
        return new Problem(url, "Statement dummy placeholder", "Dummy input format", "Dummy output format", List.of(), null, null);
    }

    @Override
    public List<String> getProblemUrlsInInterval(LocalDateTime begin, LocalDateTime end, int limit) {
        return List.of("url1", "url2", "url3", "url4");
    }
}

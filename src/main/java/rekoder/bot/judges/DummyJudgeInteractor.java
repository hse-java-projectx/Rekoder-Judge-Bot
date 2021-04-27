package rekoder.bot.judges;

import rekoder.primitive.Problem;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

public class DummyJudgeInteractor implements JudgeInteractor {
    @Override
    public String getName() {
        return "Dummy";
    }

    @Override
    public List<Problem> getAllProblems() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Problem getProblemByUrl(String url) throws IOException {
        return new Problem("A + B Problem", "Print sum of two numbers");
    }

    @Override
    public List<String> getProblemUrlsInInterval(Timestamp begin, Timestamp end) {
        return List.of("url1", "url2", "url3", "url4");
    }
}

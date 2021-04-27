package rekoder.bot.judges;

import rekoder.primitive.Problem;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.List;

public class LeetcodeInteractor implements JudgeInteractor {
    @Override
    public String getName() {
        return "Leetcode";
    }

    @Override
    public List<Problem> getAllProblems() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Problem getProblemByUrl(String url) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getProblemUrlsInInterval(Timestamp begin, Timestamp end) {
        throw new UnsupportedOperationException();
    }
}

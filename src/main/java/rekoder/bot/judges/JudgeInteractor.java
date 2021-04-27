package rekoder.bot.judges;

import rekoder.primitive.Problem;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface JudgeInteractor {
    String getName();

    List<Problem> getAllProblems();

    Problem getProblemByUrl(String url) throws IOException;

    List<String> getProblemUrlsInInterval(Timestamp begin, Timestamp end);

    default List<Problem> getProblemsInInterval(Timestamp begin, Timestamp end) throws IOException {
        final List<Problem> problems = new ArrayList<>();
        for (String url : getProblemUrlsInInterval(begin, end)) {
            problems.add(getProblemByUrl(url));
        }
        return problems;
    }
}

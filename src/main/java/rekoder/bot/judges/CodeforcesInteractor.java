package rekoder.bot.judges;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import rekoder.primitive.Problem;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.List;

public class CodeforcesInteractor implements JudgeInteractor {
    public String getName() {
        return "Codeforces";
    }

    @Override
    public List<Problem> getAllProblems() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Problem getProblemByUrl(String url) throws IOException {
        Document document = Jsoup.connect(url)
                .get();
        Element statement = document.getElementsByClass("problem-statement")
                .stream()
                .findAny()
                .orElseThrow();
        String problemName = statement.getElementsByClass("title")
                .stream()
                .findAny()
                .orElseThrow()
                .text()
                .chars()
                .dropWhile(c -> c != ' ')
                .skip(1)
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();
        String problemStatement = statement.getElementsByTag("div")
                .stream()
                .limit(12)
                .skip(11)
                .findAny()
                .orElseThrow()
                .text();

        return new Problem(problemName, problemStatement);
    }

    @Override
    public List<String> getProblemUrlsInInterval(Timestamp begin, Timestamp end) {
        throw new UnsupportedOperationException();
    }
}

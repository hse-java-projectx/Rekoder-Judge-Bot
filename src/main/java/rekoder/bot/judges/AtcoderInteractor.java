package rekoder.bot.judges;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import rekoder.primitive.Problem;
import rekoder.util.HttpRequestAttemptOverflow;
import rekoder.util.UnsupportedPageFormat;
import rekoder.util.Util;

import static rekoder.util.Util.checkNotNullOrThrowFormat;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AtcoderInteractor extends JudgeInteractor {
    public AtcoderInteractor(Logger logger) {
        super(logger, "AtCoder");
    }

    public static void main(String[] args) {
        JudgeInteractor atcoder = new AtcoderInteractor(Logger.getGlobal());
        Problem p = null;
        try {
            p = atcoder.getProblemByUrl("https://atcoder.jp/contests/agc052/tasks/agc052_c");
            System.out.printf("Name: %s\nStatement:\n%s\n", p.name, p.statement);
        } catch (IOException | UnsupportedPageFormat e) {
            e.printStackTrace();
        }
    }

    @Override
    public Problem getProblemByUrl(String url) throws IOException, UnsupportedPageFormat {
        Document problemPage;
        try {
            problemPage = Jsoup.parse(Util.executeRequest(
                    new URL(url),
                    5,
                    3000,
                    logger
            ));
        } catch (HttpRequestAttemptOverflow e) {
            throw new IOException("Can not read problem from atcoder: " + Util.formatThrowable(e));
        }
        Element title = problemPage.getElementsByClass("h2")
                .stream()
                .findAny()
                .orElse(null);
        checkNotNullOrThrowFormat(title);

        String problemName = Arrays.stream(title.text().split(" "))
                .skip(2)
                .takeWhile(s -> !s.equals("Editorial"))
                .collect(Collectors.joining(" "));

        Element statementCode = problemPage.getElementsByTag("section")
                .stream()
                .skip(12)
                .findFirst()
                .orElse(null);
        checkNotNullOrThrowFormat(statementCode);

        String problemStatement = Jsoup.parse(statementCode.html())
                .getElementsByTag("p")
                .stream()
                .map(Element::html)
                .collect(Collectors.joining("\n"));

        return new Problem(problemName, problemStatement, "atcoder input format", "atcoder output format", List.of(), null);
    }

    @Override
    public List<String> getProblemUrlsInInterval(LocalDateTime begin, LocalDateTime end, int limit) throws IOException {
        throw new UnsupportedOperationException();
    }
}

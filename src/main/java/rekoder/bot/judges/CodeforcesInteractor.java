package rekoder.bot.judges;

import org.json.JSONException;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import rekoder.primitive.Problem;
import rekoder.util.HttpRequestAttemptOverflow;
import rekoder.util.UnsupportedPageFormat;
import rekoder.util.Util;

import static rekoder.util.Util.checkNotNullOrThrowFormat;

public class CodeforcesInteractor extends JudgeInteractor {
    public static final int GET_PROBLEM_BY_URL_ATTEMPTS = 3;
    public static final int GET_PROBLEM_BY_URL_INTERVAL_SECS = 5;

    public CodeforcesInteractor(Logger logger) {
        super(logger, "Codeforces");
    }

    public static void main(String[] args) {
        JudgeInteractor interactor = new CodeforcesInteractor(Logger.getGlobal());
        try {
            Problem p = interactor.getProblemByUrl("https://codeforces.com/contest/1527/problem/E");
            System.out.println(p);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public Problem getProblemByUrl(String url) throws IOException, UnsupportedPageFormat {
        Document document;
        try {
            document = Jsoup.parse(Util.executeRequest(
                    new URL(url),
                    GET_PROBLEM_BY_URL_ATTEMPTS,
                    GET_PROBLEM_BY_URL_INTERVAL_SECS * 1000,
                    logger
            ));
        } catch (HttpRequestAttemptOverflow e) {
            throw new IOException(String.format("Unable to read codeforces problem: %s", Util.formatThrowable(e)));
        }
        Element statement = document.getElementsByClass("problem-statement")
                .stream()
                .findAny()
                .orElse(null);
        checkNotNullOrThrowFormat(statement);
        Element title = statement.getElementsByClass("title")
                .stream()
                .findAny()
                .orElse(null);
        checkNotNullOrThrowFormat(title);
        String problemName = title
                .text()
                .chars()
                .dropWhile(c -> c != ' ')
                .skip(1)
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();
        Element problemStatement = statement.getElementsByTag("div")
                .stream()
                .limit(12)
                .skip(11)
                .findAny()
                .orElse(null);
        checkNotNullOrThrowFormat(problemStatement);
        Element problemInputFormatElement = statement.getElementsByClass("input-specification")
                .stream()
                .findAny()
                .orElse(null);
        checkNotNullOrThrowFormat(problemInputFormatElement);
        String inputFormat = problemInputFormatElement.html()
                .chars()
                .skip(42)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        Element problemOutputFormat = statement.getElementsByClass("output-specification")
                .stream()
                .findAny()
                .orElse(null);
        checkNotNullOrThrowFormat(problemOutputFormat);
        String outputFormat = problemOutputFormat.html()
                .chars()
                .skip(43)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        List<String> inputs = document.getElementsByClass("sample-test")
                .stream()
                .flatMap(element -> element.getElementsByClass("input").stream())
                .map(e -> e.getElementsByTag("pre").stream().findAny().orElseThrow().html())
                .collect(Collectors.toList());
        List<String> outputs = document.getElementsByClass("sample-test")
                .stream()
                .flatMap(element -> element.getElementsByClass("output").stream())
                .map(e -> e.getElementsByTag("pre").stream().findAny().orElseThrow().html())
                .collect(Collectors.toList());

        if (inputs.size() != outputs.size()) {
            throw new UnsupportedPageFormat("Unsupported page format");
        }

        List<Problem.Test> examples = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            examples.add(new Problem.Test(inputs.get(i), outputs.get(i)));
        }

        Element contestNameElement = document.getElementsByTag("th").stream().limit(1).findAny().orElse(null);
        checkNotNullOrThrowFormat(contestNameElement);
        contestNameElement = contestNameElement.getElementsByTag("a").stream().findAny().orElse(null);
        checkNotNullOrThrowFormat(contestNameElement);
        String contestName = contestNameElement.text();

        logger.log(Level.INFO, "Got problem from codeforces: " + problemName);
        return new Problem(problemName, problemStatement.html(), inputFormat, outputFormat, examples, contestName);
    }

    @Override
    public List<String> getProblemUrlsInInterval(LocalDateTime begin, LocalDateTime end, int limit) throws IOException {
        final Map<Long, LocalDateTime> contestStartTime = new HashMap<>();
        JSONObject contestsResponse;
        try {
            contestsResponse = new JSONObject(Util.executeRequest(
                    new URL("https://codeforces.com/api/contest.list?gym=false"),
                    GET_PROBLEM_BY_URL_ATTEMPTS,
                    GET_PROBLEM_BY_URL_INTERVAL_SECS * 1000,
                    logger
            ));
        } catch (HttpRequestAttemptOverflow e) {
            throw new IOException(String.format("Unable to read codeforces contests list: %s", Util.formatThrowable(e)));
        }
        for (Object contestJsonObject : contestsResponse.getJSONArray("result")) {
            JSONObject contestJson = (JSONObject) contestJsonObject;
            long startTimeSeconds = contestJson.getLong("startTimeSeconds");
            String type = contestJson.getString("type");
            if (!type.equals("CF")) {
                continue;
            }
            LocalDateTime startDate = LocalDateTime.ofEpochSecond(startTimeSeconds, 0, ZoneOffset.UTC);
            long contestId = contestJson.getLong("id");
            contestStartTime.put(contestId, startDate);
        }

        logger.log(Level.INFO, String.format("Found %d contests on codeforces", contestStartTime.size()));
        List<String> problemUrls = new ArrayList<>();

        JSONObject problemsResponse;
        try {
            problemsResponse = new JSONObject(Util.executeRequest(
                    new URL("https://codeforces.com/api/problemset.problems"),
                    GET_PROBLEM_BY_URL_ATTEMPTS,
                    GET_PROBLEM_BY_URL_INTERVAL_SECS * 1000,
                    logger
            ));
        } catch (HttpRequestAttemptOverflow e) {
            throw new IOException(String.format("Unable to read codeforces problems list: %s", Util.formatThrowable(e)));
        }

        for (Object problemJsonObject : problemsResponse.getJSONObject("result").getJSONArray("problems")) {
            JSONObject problemJson = (JSONObject) problemJsonObject;
            try {
                long problemSourceContestId = problemJson.getLong("contestId");
                if (!contestStartTime.containsKey(problemSourceContestId)) {
                    logger.log(Level.INFO, "Can not find source contest '" + problemSourceContestId + "'");
                    continue;
                }
                LocalDateTime problemCreationDate = contestStartTime.get(problemSourceContestId);
                if (!(problemCreationDate.isAfter(begin) && problemCreationDate.isBefore(end))) {
                    continue;
                }
                String problemIndex = problemJson.getString("index");
                if (limit != NO_LIMIT && problemUrls.size() == limit) {
                    break;
                }
                problemUrls.add(String.format("https://codeforces.com/contest/%d/problem/%s", problemSourceContestId, problemIndex));
            } catch (JSONException ignored) {
            }
        }

        logger.log(Level.INFO, String.format("Got %d new problems from Codeforces", problemUrls.size()));
        return problemUrls;
    }
}

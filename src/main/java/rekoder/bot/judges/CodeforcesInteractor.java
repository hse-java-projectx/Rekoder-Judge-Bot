package rekoder.bot.judges;

import org.json.JSONException;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import rekoder.primitive.Problem;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import rekoder.util.HttpRequestAttemptOverflow;
import rekoder.util.UnsupportedPageFormat;
import rekoder.util.Util;

public class CodeforcesInteractor extends JudgeInteractor {
    public static final int GET_PROBLEM_BY_URL_ATTEMPTS = 3;
    public static final int GET_PROBLEM_BY_URL_INTERVAL_SECS = 5;

    public CodeforcesInteractor(Logger logger) {
        super(logger, "Codeforces");
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
        logger.log(Level.INFO, "Got problem from codeforces: " + problemName);
        return new Problem(problemName, problemStatement.text());
    }

    public static void main(String[] args) {
        JudgeInteractor cf = new CodeforcesInteractor(Logger.getGlobal());
        try {
            System.out.println(cf.getProblemByUrl("https://codeforces.com/contest/949/problem/F"));
        } catch (IOException | UnsupportedPageFormat e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getProblemUrlsInInterval(LocalDateTime begin, LocalDateTime end) throws IOException {
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
                problemUrls.add(String.format("https://codeforces.com/contest/%d/problem/%s", problemSourceContestId, problemIndex));
            } catch (JSONException ignored) {
            }
        }

        logger.log(Level.INFO, String.format("Got %d new problems from Codeforces", problemUrls.size()));
        return problemUrls;
    }

    private void checkNotNullOrThrowFormat(Object o) throws UnsupportedPageFormat {
        if (o == null) {
            throw new UnsupportedPageFormat("Unsupported page format");
        }
    }
}

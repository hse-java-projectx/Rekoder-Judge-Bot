package rekoder.bot.judges;

import rekoder.primitive.Problem;
import rekoder.util.UnsupportedPageFormat;
import rekoder.util.Util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class JudgeInteractor {
    public static int NO_LIMIT = -1;

    protected final Logger logger;
    private final String name;

    public JudgeInteractor(Logger logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Problem> getAllProblems() throws IOException {
        return getProblemsInInterval(LocalDateTime.MIN, LocalDateTime.MAX, NO_LIMIT);
    }

    public abstract Problem getProblemByUrl(String url) throws IOException, UnsupportedPageFormat;

    public abstract List<String> getProblemUrlsInInterval(LocalDateTime begin, LocalDateTime end, int limit) throws IOException;

    public List<Problem> getProblemsInInterval(LocalDateTime begin, LocalDateTime end, int limit) throws IOException {
        final List<Problem> problems = new ArrayList<>();
        for (String url : getProblemUrlsInInterval(begin, end, limit)) {
            try {
                problems.add(getProblemByUrl(url));
            } catch (UnsupportedPageFormat e) {
                logger.log(Level.INFO, String.format("Problem page format is not supported: %s", Util.formatThrowable(e)));
            }
        }
        return problems;
    }
}

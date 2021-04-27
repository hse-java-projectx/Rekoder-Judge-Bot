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
        return getProblemsInInterval(LocalDateTime.MIN, LocalDateTime.MAX);
    }

    public abstract Problem getProblemByUrl(String url) throws IOException, UnsupportedPageFormat;

    public abstract List<String> getProblemUrlsInInterval(LocalDateTime begin, LocalDateTime end) throws IOException;

    public List<Problem> getProblemsInInterval(LocalDateTime begin, LocalDateTime end) throws IOException {
        final List<Problem> problems = new ArrayList<>();
        for (String url : getProblemUrlsInInterval(begin, end)) {
            try {
                problems.add(getProblemByUrl(url));
            } catch (UnsupportedPageFormat e) {
                logger.log(Level.INFO, String.format("Problem page format is not supported: %s", Util.formatThrowable(e)));
            }
        }
        return problems;
    }
}

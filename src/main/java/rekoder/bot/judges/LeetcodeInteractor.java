package rekoder.bot.judges;

import rekoder.primitive.Problem;
import rekoder.util.HttpRequestAttemptOverflow;
import rekoder.util.UnsupportedPageFormat;
import rekoder.util.Util;

import org.jsoup.nodes.Document;
import org.jsoup.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;


public class LeetcodeInteractor extends JudgeInteractor {
    public LeetcodeInteractor(Logger logger) {
        super(logger, "Leetcode");
    }

    @Override
    public List<Problem> getAllProblems() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Problem getProblemByUrl(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getProblemUrlsInInterval(LocalDateTime begin, LocalDateTime end, int limit) {
        throw new UnsupportedOperationException();
    }
}

package rekoder.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {
    public static String executeRequest(URL url, int attempts, int intervalMillis, Logger logger) throws HttpRequestAttemptOverflow {
        for (int attempt = 0; attempt < attempts; ++attempt) {
            InputStream is;
            try {
                is = url.openStream();
                return new String(is.readAllBytes());
            } catch (IOException e) {
                try {
                    Thread.sleep(intervalMillis, 0);
                    logger.log(Level.INFO, String.format("Attempting again: %s", url));
                } catch (InterruptedException ignored) {
                    throw new HttpRequestAttemptOverflow(url.toString());
                }
            }
        }
        throw new HttpRequestAttemptOverflow(url.toString());
    }

    public static String formatThrowable(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return String.format("message: %s\ntrace:\n%s", t.getMessage(), sw);
    }

    public static void checkNotNullOrThrowFormat(Object o) throws UnsupportedPageFormat {
        if (o == null) {
            throw new UnsupportedPageFormat("Unsupported page format");
        }
    }
}

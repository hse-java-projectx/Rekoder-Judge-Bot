package rekoder;

import java.util.logging.Level;

public class ResultOrError<R> {
    public final boolean isError;
    private final String message;
    private final Level level;
    private final R result;

    public ResultOrError(R res) {
        this.isError = false;
        this.message = "";
        this.level = Level.OFF;
        this.result = res;
    }

    public ResultOrError(Level errorLevel, String message) {
        this.isError = true;
        this.message = message;
        this.level = errorLevel;
        this.result = null;
    }

    public R getResult() {
        return result;
    }

    public String getErrorMessage() {
        return message;
    }
}

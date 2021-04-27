package rekoder.api;

import rekoder.primitive.Problem;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RekoderApiImplOffline implements RekoderApi {
    private final Logger logger;

    public RekoderApiImplOffline(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void addProblem(Problem problem, Consumer<String> callback) {
        logger.log(Level.INFO, String.format("Add problem: %s", problem.name));
    }
}

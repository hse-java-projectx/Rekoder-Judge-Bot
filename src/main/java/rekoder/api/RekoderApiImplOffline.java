package rekoder.api;

import rekoder.primitive.Problem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
        try {
            Files.createDirectories(Path.of("temp"));
            Path outputFile = Paths.get("temp", problem.name);
            Files.createFile(outputFile);
            Files.write(outputFile, Collections.singleton(problem.statement));
            logger.log(Level.INFO, String.format("Add offline problem: %s", problem.name));
        } catch (IOException e) {
            callback.accept(e.getMessage());
        }
    }
}

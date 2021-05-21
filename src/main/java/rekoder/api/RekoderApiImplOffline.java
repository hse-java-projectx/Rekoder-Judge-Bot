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
    public int addProblem(String user, Problem problem) throws IOException {
        Files.createDirectories(Path.of("temp"));
        Path outputFile = Paths.get("temp", problem.name);
        Files.createFile(outputFile);
        Files.write(outputFile, Collections.singleton(problem.statement));
        logger.log(Level.INFO, String.format("Add offline problem: %s", problem.name));
        return 0;
    }

    @Override
    public void putProblem(int folderId, int problemId) {

    }

    @Override
    public int addFolder(int parentFolder, String name) throws IOException {
        Files.createDirectories(Path.of(name));
        return 0;
    }

    @Override
    public int getUserRootFolderId(String user) {
        return 0;
    }
}

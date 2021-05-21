package rekoder.api;

import rekoder.primitive.Problem;

import java.io.IOException;

public interface RekoderApi {
    int addProblem(String user, Problem problem) throws IOException;

    void putProblem(int folderId, int problemId) throws IOException;

    int addFolder(int parentFolder, String name) throws IOException;

    int getUserRootFolderId(String user) throws IOException;
}

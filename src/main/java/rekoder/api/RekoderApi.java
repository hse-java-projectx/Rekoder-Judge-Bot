package rekoder.api;

import rekoder.primitive.Problem;

import java.util.function.Consumer;

public interface RekoderApi {
    void addProblem(Problem problem, Consumer<String> callback);
}

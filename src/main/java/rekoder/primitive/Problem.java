package rekoder.primitive;

import java.util.List;
import java.util.stream.Collectors;

public class Problem {
    public final String name;
    public final String statement;
    public final String inputFormat;
    public final String outputFormat;
    public final List<Test> examples;
    public final String contest;

    public Problem(String name, String statement, String inputFormat, String outputFormat, List<Test> examples, String contest) {
        this.name = name;
        this.statement = statement;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.examples = examples;
        this.contest = contest;
    }

    public static class Test {
        public final String input;
        public final String output;

        public Test(String input, String output) {
            this.input = input;
            this.output = output;
        }
    }

    @Override
    public int hashCode() {
        return this.name.hashCode()
                ^ this.statement.hashCode()
                ^ this.inputFormat.hashCode()
                ^ this.outputFormat.hashCode()
                ^ this.examples.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Problem)) {
            return false;
        }
        Problem other = (Problem) o;
        return this.name.equals(other.name)
                && this.statement.equals(other.statement)
                && this.inputFormat.equals(other.inputFormat)
                && this.outputFormat.equals(other.outputFormat)
                && this.examples.equals(other.examples);
    }

    @Override
    public String toString() {
        return String.format("Name: %s\nStatement: %s\nInput: %s\nOutput: %s\nTests: %s",
                name,
                statement,
                inputFormat,
                outputFormat,
                examples.stream()
                        .map(t -> String.format("i> '%s'\no> '%s'\n", t.input, t.output))
                        .collect(Collectors.joining("\n")));
    }
}

package rekoder.bot.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandLineInterface implements Runnable {
    private final String name;
    private final Map<String, Function<String[], CommandHandlerResult>> handlers;
    private final String exitCommand;
    private final Logger logger;

    public CommandLineInterface(String name,
                                String exitCommand,
                                Map<String, Function<String[], CommandHandlerResult>> handlers,
                                Logger logger) {
        this.name = name;
        this.handlers = handlers;
        this.exitCommand = exitCommand;
        this.logger = logger;
    }

    public static void main(String[] args) {
        var cli = new CommandLineInterface(
                "bot",
                "exit",
                Map.of(
                        "print", (String[] a) -> new CommandHandlerResult(false, "print is called!"),
                        "raise", (String[] a) -> new CommandHandlerResult(true, "error is raised!")
                ), Logger.getGlobal());
        cli.run();
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        while (true) {
            System.out.printf("%s> ", name);
            System.out.flush();
            String userInput;
            try {
                userInput = reader.readLine();
                System.out.println();
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage());
                continue;
            }
            final String[] args = userInput.split(" ");
            if (args.length == 0) {
                continue;
            }
            final String command = args[0];
            final String[] params = Arrays.copyOfRange(args, 1, args.length);
            if (userInput.equals(exitCommand)) {
                break;
            }
            if (!handlers.containsKey(command)) {
                System.out.printf("Unknown command: '%s'\n", userInput);
                continue;
            }
            Function<String[], CommandHandlerResult> handler = handlers.get(command);
            CommandHandlerResult result = handler.apply(params);
            if (result.hasError) {
                System.out.printf("Error: %s\n", result.message);
                logger.log(Level.WARNING, String.format("Error in %s: '%s'", name, result.message));
            } else {
                System.out.println(result.message);
            }
        }
        try {
            reader.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public static class CommandHandlerResult {
        public final boolean hasError;
        public final String message;

        public CommandHandlerResult(boolean hasError, String message) {
            this.hasError = hasError;
            this.message = message;
        }
    }
}

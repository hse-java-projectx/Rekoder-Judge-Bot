package rekoder.bot.cli;

import rekoder.ResultOrError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CommandLineInterface implements Runnable {
    private static final String EXIT_COMMAND = "exit";
    private static final String HELP_COMMAND = "help";

    private final String name;
    private final Map<String, CliHandler> handlers;
    private final Logger logger;
    private boolean exitFlag = false;

    public CommandLineInterface(String name,
                                Map<String, CliHandler> handlersWithoutExitHelp,
                                Logger logger) {
        Map<String, CliHandler> extendedHandlers = new HashMap<>(handlersWithoutExitHelp);

        extendedHandlers.put(EXIT_COMMAND, new CliHandler() {
            @Override
            public ResultOrError<String> handle(String[] args) {
                exitFlag = true;
                return new ResultOrError<>("");
            }

            @Override
            public String getHelp() {
                return "Exit from CLI";
            }

            @Override
            public List<String> getParams() {
                return List.of();
            }
        });

        extendedHandlers.put(HELP_COMMAND, new CliHandler() {
            @Override
            public ResultOrError<String> handle(String[] args) {
                return new ResultOrError<>(
                        handlers.entrySet().stream()
                                .map(entry -> String.format("'%s' - %s",
                                        entry.getKey() + (entry.getValue().getParams().isEmpty() ? "" : " ")
                                                + entry.getValue().getParams()
                                                .stream()
                                                .map(s -> "<" + s + ">")
                                                .collect(Collectors.joining(" ")),
                                        entry.getValue().getHelp()))
                                .collect(Collectors.joining("\n")));
            }

            @Override
            public String getHelp() {
                return "Get CLI help";
            }

            @Override
            public List<String> getParams() {
                return List.of();
            }
        });

        this.name = name;
        this.handlers = extendedHandlers;
        this.logger = logger;
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        while (!exitFlag) {
            System.out.printf("%s> ", name);
            System.out.flush();
            String userInput;
            try {
                userInput = reader.readLine();
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
            if (!handlers.containsKey(command)) {
                System.out.printf("Unknown command: '%s'\n", userInput);
                continue;
            }
            CliHandler handler = handlers.get(command);
            ResultOrError<String> result = handler.handle(params);
            if (result.isError) {
                System.out.printf("Error: %s\n", result.getErrorMessage());
                logger.log(Level.WARNING, String.format("Error in %s: '%s'", name, result.getErrorMessage()));
            } else {
                if (!result.getResult().isEmpty()) {
                    System.out.println(result.getResult());
                }
            }
        }
        try {
            reader.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public interface CliHandler {
        ResultOrError<String> handle(String[] args);

        String getHelp();

        List<String> getParams();
    }
}

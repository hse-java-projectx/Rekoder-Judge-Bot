package rekoder.bot;

import rekoder.ResultOrError;
import rekoder.api.RekoderApi;
import rekoder.api.RekoderApiImplOffline;
import rekoder.bot.cli.CommandLineInterface;
import rekoder.bot.judges.CodeforcesInteractor;
import rekoder.bot.judges.DummyJudgeInteractor;
import rekoder.bot.judges.JudgeInteractor;
import rekoder.bot.judges.LeetcodeInteractor;
import rekoder.primitive.Problem;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RekoderBot implements Runnable {
    private final Map<String, JudgeInteractorWrapper> interactors;
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingDeque<>();
    private final RekoderApi api;
    private final Logger logger;

    public static void main(String[] args) {
        var bot = new RekoderBot(
                List.of(
                        new CodeforcesInteractor(),
                        new LeetcodeInteractor(),
                        new DummyJudgeInteractor()
                ),
                new RekoderApiImplOffline(Logger.getGlobal()),
                Logger.getGlobal());
        bot.run();
    }

    public RekoderBot(List<JudgeInteractor> interactors, RekoderApi api, Logger logger) {
        this.api = api;
        this.logger = logger;
        this.interactors = new HashMap<>();
        for (JudgeInteractor interactor : interactors) {
            this.interactors.put(interactor.getName(), new JudgeInteractorWrapper(
                    Timestamp.valueOf(LocalDateTime.MIN),
                    interactor,
                    JudgeInteractorStatus.NEVER_UPDATED
            ));
        }
    }

    @Override
    public void run() {
        Map<String, CommandLineInterface.CliHandler> cliCommands = Map.of(
                "list", new ListHandler(),
                "update", new UpdateHandler(),
                "tasks", new TasksHandler()
        );

        ExecutorService executorService = new ThreadPoolExecutor(
                1,
                10,
                10,
                TimeUnit.SECONDS,
                new LinkedTransferQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Thread cli = new Thread(new CommandLineInterface(
                "RJB",
                cliCommands,
                Logger.getGlobal()));
        Thread taskExecutor = new Thread(new TaskExecutor(executorService));

        taskExecutor.start();
        cli.start();
        try {
            cli.join();
            taskExecutor.interrupt();
            taskExecutor.join();
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Unexpected interrupt: " + e.getMessage());
        }

        executorService.shutdown();
    }

    private class TaskExecutor implements Runnable {
        private final ExecutorService executor;

        private TaskExecutor(ExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    var task = tasks.take();
                    executor.submit(task);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private class UpdateHandler implements CommandLineInterface.CliHandler {
        @Override
        public ResultOrError<String> handle(String[] args) {
            if (args.length < 1) {
                return new ResultOrError<>(Level.INFO, "Judge name is required as first argument");
            }
            String judgeName = args[0];
            if (!interactors.containsKey(judgeName)) {
                return new ResultOrError<>(Level.INFO, "Judge does not exist: '" + judgeName + "'");
            }
            JudgeInteractor interactor = interactors.get(judgeName).interactor;
            Timestamp lastUpdate = interactors.get(judgeName).lastUpdate;
            assert interactors.get(judgeName) != null;

            tasks.add(() -> {
                try {
                    Timestamp curTime = Timestamp.valueOf(LocalDateTime.now());
                    List<Problem> problemList = interactor.getProblemsInInterval(lastUpdate, curTime);
                    interactors.put(judgeName, new JudgeInteractorWrapper(curTime, interactor, JudgeInteractorStatus.UPDATED));
                    problemList.forEach(problem -> tasks.add(() -> {
                        synchronized (api) {
                            api.addProblem(problem, getErrorLoggingCallback());
                        }
                    }));
                } catch (IOException e) {
                    logger.log(Level.WARNING, String.format("Update was not successful: %s", e.getMessage()));
                } catch (UnsupportedOperationException e) {
                    logger.log(
                            Level.INFO,
                            String.format("Interactor '%s' does not support this operation, stack trace:\n%s",
                                    interactor.getName(),
                                    Arrays.stream(e.getStackTrace())
                                            .map(StackTraceElement::toString)
                                            .collect(Collectors.joining("\n"))));
                }
            });

            return new ResultOrError<>("");
        }

        @Override
        public String getHelp() {
            return "Fetch recent problems from judge";
        }

        @Override
        public List<String> getParams() {
            return List.of("judge name");
        }
    }

    private Consumer<String> getErrorLoggingCallback() {
        return error -> logger.log(Level.WARNING, "Error callback: " + error);
    }

    private class ListHandler implements CommandLineInterface.CliHandler {
        @Override
        public ResultOrError<String> handle(String[] args) {
            return new ResultOrError<>(
                    interactors
                            .values()
                            .stream()
                            .map(i -> i.interactor.getName() + ", updated: " + i.getUpdateString())
                            .collect(Collectors.joining("\n"))
            );
        }

        @Override
        public String getHelp() {
            return "Get list of all active judge interactors";
        }

        @Override
        public List<String> getParams() {
            return List.of();
        }
    }

    private class TasksHandler implements CommandLineInterface.CliHandler {
        @Override
        public ResultOrError<String> handle(String[] args) {
            return new ResultOrError<>(String.format("Queued tasks: %d", tasks.size()));
        }

        @Override
        public String getHelp() {
            return "Get queued tasks count";
        }

        @Override
        public List<String> getParams() {
            return List.of();
        }
    }

    private enum JudgeInteractorStatus {
        NEVER_UPDATED,
        UPDATED
    }

    private static class JudgeInteractorWrapper {
        public final Timestamp lastUpdate;
        public final JudgeInteractor interactor;
        public JudgeInteractorStatus status;

        public JudgeInteractorWrapper(Timestamp lastUpdate, JudgeInteractor interactor, JudgeInteractorStatus status) {
            this.lastUpdate = lastUpdate;
            this.interactor = interactor;
            this.status = status;
        }

        public String getUpdateString() {
            if (status == JudgeInteractorStatus.NEVER_UPDATED) {
                return "never";
            }
            return lastUpdate.toString();
        }
    }
}

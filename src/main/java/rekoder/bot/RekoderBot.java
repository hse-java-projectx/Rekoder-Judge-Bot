package rekoder.bot;

import rekoder.ResultOrError;
import rekoder.api.RekoderApi;
import rekoder.api.RekoderApiOnline;
import rekoder.bot.cli.CommandLineInterface;
import rekoder.bot.judges.CodeforcesInteractor;
import rekoder.bot.judges.DummyJudgeInteractor;
import rekoder.bot.judges.JudgeInteractor;
import rekoder.bot.judges.LeetcodeInteractor;
import rekoder.primitive.Problem;
import rekoder.util.Util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RekoderBot implements Runnable {
    private final Map<String, JudgeInteractorWrapper> interactors;
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingDeque<>();
    private final Supplier<RekoderApi> apiSupplier;
    private final Logger logger;
    private final int PROBLEMS_LIMIT = 1200;

    public static void main(String[] args) {
        var bot = new RekoderBot(
                List.of(
                        new CodeforcesInteractor(Logger.getGlobal()),
                        new LeetcodeInteractor(Logger.getGlobal()),
                        new DummyJudgeInteractor(Logger.getGlobal())
                ),
                () -> new RekoderApiOnline(Logger.getGlobal()),
                Logger.getGlobal());
        bot.run();
    }

    public RekoderBot(List<JudgeInteractor> interactors, Supplier<RekoderApi> apiSupplier, Logger logger) {
        this.apiSupplier = apiSupplier;
        this.logger = logger;
        this.interactors = new HashMap<>();
        for (JudgeInteractor interactor : interactors) {
            this.interactors.put(interactor.getName(), new JudgeInteractorWrapper(interactor));
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
            LocalDateTime lastUpdate = interactors.get(judgeName).lastUpdate;
            assert interactors.get(judgeName) != null;

            tasks.add(() -> {
                try {
                    LocalDateTime curTime = LocalDateTime.now();
                    List<Problem> problemList = interactor.getProblemsInInterval(lastUpdate, curTime, PROBLEMS_LIMIT);
                    interactors.get(judgeName).update(curTime);

                    final String localJudgeName = "CF3";

                    RekoderApi api = apiSupplier.get();
                    int judgeRootId = api.getUserRootFolderId(localJudgeName);

                    Set<String> allContests = problemList.stream()
                            .flatMap(problem -> Optional.ofNullable(problem.contest).stream())
                            .collect(Collectors.toUnmodifiableSet());
                    Map<String, Integer> folderId = new ConcurrentHashMap<>();
                    {
                        List<Runnable> addFolderTasks = new ArrayList<>();
                        for (String contestName : allContests) {
                            addFolderTasks.add(() -> {
                                try {
                                    folderId.put(contestName, api.addFolder(judgeRootId, contestName));
                                } catch (IOException e) {
                                    logger.warning(String.format("Failed to add folder '%s' to %s. %s", contestName, judgeName, e));
                                }
                            });
                        }
                        executeAllWithThreadPool(addFolderTasks);
                    }

                    Set<Problem> uniqueProblems = new HashSet<>(problemList);
                    Map<Problem, Integer> problemId = new ConcurrentHashMap<>();
                    {
                        List<Runnable> addProblemTasks = new ArrayList<>();
                        for (Problem uniqueProblem : uniqueProblems) {
                            addProblemTasks.add(() -> {
                                try {
                                    problemId.put(uniqueProblem, api.addProblem(localJudgeName, uniqueProblem));
                                } catch (IOException e) {
                                    logger.warning(String.format("Failed to add problem %s to %s. %s", uniqueProblem.name, judgeName, e));
                                }
                            });
                        }
                        executeAllWithThreadPool(addProblemTasks);
                    }

                    {
                        List<Runnable> putProblemTasks = new ArrayList<>();
                        for (Problem problem : problemList) {
                            putProblemTasks.add(() -> {
                                if (problem.contest != null && !folderId.containsKey(problem.contest)) {
                                    logger.warning(String.format("Failed to put problem %s to %s. Folder was not added",
                                            problem.name,
                                            problem.contest));
                                } else {
                                    int folderToAddProblemId = problem.contest == null
                                            ? judgeRootId
                                            : folderId.get(problem.contest);
                                    try {
                                        api.putProblem(folderToAddProblemId, problemId.get(problem));
                                    } catch (IOException e) {
                                        logger.warning(String.format("Failed to put problem %s to %d. %s", problem.name, judgeRootId, e));
                                    }
                                }
                            });
                        }
                        executeAllWithThreadPool(putProblemTasks);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, String.format("Update was not successful: %s", Util.formatThrowable(e)));
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

    private void executeAllWithThreadPool(List<Runnable> tasks) {
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (Runnable task : tasks) {
            futures.add(executorService.submit(task));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.info("Future.get was interrupted");
            }
        }
    }

    private static class JudgeInteractorWrapper {
        public LocalDateTime lastUpdate;
        public final JudgeInteractor interactor;

        public JudgeInteractorWrapper(JudgeInteractor interactor) {
            this.lastUpdate = LocalDateTime.MIN;
            this.interactor = interactor;
        }

        public void update(LocalDateTime time) {
            this.lastUpdate = time;
        }

        public String getUpdateString() {
            if (lastUpdate.equals(LocalDateTime.MIN)) {
                return "never";
            }
            return lastUpdate.toString();
        }
    }
}

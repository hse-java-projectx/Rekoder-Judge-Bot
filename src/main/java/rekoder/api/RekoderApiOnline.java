package rekoder.api;

import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import rekoder.primitive.Problem;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RekoderApiOnline implements RekoderApi {
    private final Logger logger;

    public RekoderApiOnline(Logger logger) {
        this.logger = logger;
    }

    public static void main(String[] args) {
        RekoderApi api = new RekoderApiOnline(Logger.getGlobal());
        try {
            int id = api.addFolder(api.getUserRootFolderId("Glebanister"), "Hello from Java");
            api.addProblem(
                    "Glebanister",
                    new Problem(
                            "A + B Problem",
                            "Hello, dudes",
                            "input format",
                            "output format",
                            List.of(new Problem.Test("input1", "output1")),
                            null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int addProblem(String user, Problem problem) throws IOException {
        JSONObject problemJson = new JSONObject();
        problemJson.put("name", problem.name);
        problemJson.put("statement", problem.statement);
        problemJson.put("inputFormat", problem.inputFormat);
        problemJson.put("outputFormat", problem.outputFormat);
        problemJson.put("tests", new JSONArray(problem.examples.stream().map(test -> new JSONObject(Map.of(
                "input", test.input,
                "output", test.output
        ))).collect(Collectors.toList())));
        String postProblemResponse;
        postProblemResponse = postJsonBody(ApiUrl.addProblem(user), problemJson.toString());
        JSONObject postedProblem = new JSONObject(postProblemResponse);
        return postedProblem.getInt("id");
    }

    @Override
    public void putProblem(int folderId, int problemId) throws IOException {
        JSONObject putProblemToFolderJson = new JSONObject(Map.of("problemId", problemId));
        patchJsonBody(ApiUrl.putProblem(folderId), putProblemToFolderJson.toString());
    }

    @Override
    public int addFolder(int parentFolder, String name) throws IOException {
        JSONObject addFolderJson = new JSONObject(Map.of("name", name));
        return new JSONObject(postJsonBody(ApiUrl.addFolder(parentFolder), addFolderJson.toString())).getInt("id");
    }

    @Override
    public int getUserRootFolderId(String user) throws IOException {
        return getJsonBody(ApiUrl.getUser(user)).getInt("rootFolderId");
    }

    private String postJsonBody(String url, String body) throws IOException {
        return executeHttpRequest(new HttpPost(url), body);
    }

    private void patchJsonBody(String url, String body) throws IOException {
        executeHttpRequest(new HttpPatch(url), body);
    }

    private JSONObject getJsonBody(String url) throws IOException {
        return new JSONObject(executeHttpRequest(new HttpGet(url)));
    }

    private String executeHttpRequest(HttpRequestBase request, String body) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            if (request instanceof HttpEntityEnclosingRequestBase) {
                StringEntity entity = new StringEntity(body);
                ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
            }
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            logger.info(String.format("API: request %s, body: %s", request, body));
            try (CloseableHttpResponse response = client.execute(request)) {
                logger.info(String.format("API: response %s", response));
                int responseCode = response.getStatusLine().getStatusCode();
                if (responseCode == 204) {
                    return new JSONObject().toString();
                }
                if (responseCode / 100 != 2) {
                    throw new IOException(String.format("API Request error, code %d: %s",
                            response.getStatusLine().getStatusCode(),
                            response.getStatusLine().getReasonPhrase()));
                }
                return readAllInputStream(response.getEntity().getContent());
            }
        }
    }

    private String executeHttpRequest(HttpRequestBase request) throws IOException {
        return executeHttpRequest(request, new JSONObject().toString());
    }

    private String readAllInputStream(InputStream is) {
        return new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));
    }

    private static class ApiUrl {
        private static final String DOMAIN = "https://rekoderback.cfapps.eu10.hana.ondemand.com";

        private static String addProblem(String user) {
            return String.format("%s/users/%s/problems", DOMAIN, user);
        }

        private static String putProblem(int folder) {
            return String.format("%s/folders/%s/problems", DOMAIN, folder);
        }

        private static String addFolder(int folder) {
            return String.format("%s/folders/%s/folders", DOMAIN, folder);
        }

        private static String getUser(String user) {
            return String.format("%s/users/%s", DOMAIN, user);
        }
    }
}

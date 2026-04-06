/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark.serde;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.utils.Logger;

/**
 * Converts JMH result JSON (produced by {@code -rf json -rff results.json})
 * into the
 * cross-language output schema format for serde benchmark comparison.
 *
 * <p>
 * JMH SampleTime mode results contain:
 * <ul>
 * <li>{@code primaryMetric.score} &rarr; mean (in nanoseconds with
 * {@code @OutputTimeUnit(NANOSECONDS)})</li>
 * <li>{@code primaryMetric.scoreError} &rarr; std_dev approximation</li>
 * <li>{@code primaryMetric.scorePercentiles} &rarr; p50, p90, p95, p99</li>
 * <li>{@code measurementIterations} &rarr; n</li>
 * </ul>
 *
 * <p>
 * The test case ID is extracted from the {@code params.testCaseId} field of
 * each benchmark result.
 */
public final class JmhResultConverter {

    private static final Logger log = Logger.loggerFor(JmhResultConverter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JmhResultConverter() {
    }

    /**
     * Read JMH results from {@code inputPath}, convert to the cross-language output
     * schema, and write both JSON and Markdown files using the given output prefix.
     *
     * <p>Produces two files: {@code <outputPrefix>.json} and {@code <outputPrefix>.md}.
     *
     * @param inputPath    path to the JMH JSON result file
     * @param outputPrefix path prefix for output files (without extension)
     */
    public static void convert(String inputPath, String outputPrefix) {
        try {
            JsonNode jmhResults = MAPPER.readTree(new File(inputPath));
            ObjectNode output = buildOutput(jmhResults);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(outputPrefix + ".json"), output);
            writeMarkdown(output, new File(outputPrefix + ".md"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JMH results: " + e.getMessage(), e);
        }
    }

    static ObjectNode buildOutput(JsonNode jmhResults) {
        ObjectNode output = MAPPER.createObjectNode();
        output.set("metadata", buildMetadata());
        output.set("serde_benchmarks", buildBenchmarkEntries(jmhResults));
        return output;
    }

    private static ObjectNode buildMetadata() {
        ObjectNode metadata = MAPPER.createObjectNode();
        metadata.put("lang", "Java");

        ArrayNode software = MAPPER.createArrayNode();
        software.add(toJsonArray("AWS SDK for Java", VersionInfo.SDK_VERSION));
        metadata.set("software", software);

        String os = System.getProperty("os.name", "unknown") + " "
                + System.getProperty("os.version", "") + " "
                + System.getProperty("os.arch", "");
        metadata.put("os", os.trim());
        metadata.put("instance", "TODO");
        metadata.put("precision", "-9");
        return metadata;
    }

    private static ArrayNode toJsonArray(String... values) {
        ArrayNode array = MAPPER.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static ArrayNode buildBenchmarkEntries(JsonNode jmhResults) {
        ArrayNode entries = MAPPER.createArrayNode();
        if (jmhResults == null || !jmhResults.isArray()) {
            return entries;
        }
        for (JsonNode result : jmhResults) {
            ObjectNode entry = convertSingleResult(result);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static ObjectNode convertSingleResult(JsonNode result) {
        String testCaseId = extractTestCaseId(result);
        if (testCaseId == null) {
            return null;
        }

        long n = computeTotalInvocations(result.path("primaryMetric").path("rawDataHistogram"));
        JsonNode primaryMetric = result.path("primaryMetric");
        double mean = primaryMetric.path("score").asDouble(0.0);
        double stdDev = primaryMetric.path("scoreError").asDouble(0.0);

        JsonNode percentiles = primaryMetric.path("scorePercentiles");
        double p50 = percentiles.path("50.0").asDouble(0.0);
        double p90 = percentiles.path("90.0").asDouble(0.0);
        double p95 = percentiles.path("95.0").asDouble(0.0);
        double p99 = percentiles.path("99.0").asDouble(0.0);

        ObjectNode entry = MAPPER.createObjectNode();
        entry.put("id", testCaseId);
        entry.put("n", n);
        entry.put("mean", mean);
        entry.put("p50", p50);
        entry.put("p90", p90);
        entry.put("p95", p95);
        entry.put("p99", p99);
        entry.put("std_dev", stdDev);
        return entry;
    }

    /**
     * Compute the total number of invocations from the rawDataHistogram.
     *
     * <p>
     * The histogram structure is:
     * {@code [fork][iteration] = list of [value, count]}.
     * The total N is the sum of all counts across all forks and iterations.
     * </p>
     */
    private static long computeTotalInvocations(JsonNode rawDataHistogram) {
        if (rawDataHistogram.isMissingNode() || !rawDataHistogram.isArray()) {
            return 0;
        }
        long total = 0;
        for (JsonNode fork : rawDataHistogram) {
            if (!fork.isArray()) {
                continue;
            }
            for (JsonNode iteration : fork) {
                if (!iteration.isArray()) {
                    continue;
                }
                for (JsonNode bin : iteration) {
                    if (bin.isArray() && bin.size() >= 2) {
                        total += bin.get(1).asLong(0);
                    }
                }
            }
        }
        return total;
    }

    private static String extractTestCaseId(JsonNode result) {
        JsonNode params = result.path("params");
        JsonNode testCaseIdNode = params.path("testCaseId");
        if (testCaseIdNode.isMissingNode() || !testCaseIdNode.isTextual()) {
            return null;
        }
        return testCaseIdNode.asText();
    }

    /**
     * Write the converted output as a rendered Markdown table.
     *
     * <p>The format matches the cross-language reference, e.g.:
     * <pre>
     * # Java
     *
     * ## Linux 5.15.0 x86_64 m7g.xlarge
     *
     * ```
     * AWS SDK for Java / 2.x.y
     * ```
     * |id|n|mean|p50|p90|p95|p99|std_dev|
     * |----:|----:|----:|----:|----:|----:|----:|----:|
     * |awsJson1_0_...|1,234|5,678|...|
     * </pre>
     */
    static void writeMarkdown(ObjectNode output, File file) throws IOException {
        JsonNode metadata = output.path("metadata");
        JsonNode benchmarks = output.path("serde_benchmarks");

        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);

        try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
            // Header: # <lang>
            pw.println("# " + metadata.path("lang").asText("Java"));
            pw.println();

            // Sub-header: ## <os> <instance>
            String os = metadata.path("os").asText("");
            String instance = metadata.path("instance").asText("");
            pw.println("## " + (os + " " + instance).trim());
            pw.println();
            pw.println();

            // Software block
            JsonNode software = metadata.path("software");
            if (software.isArray() && software.size() > 0) {
                pw.println("```");
                for (JsonNode entry : software) {
                    if (entry.isArray() && entry.size() >= 2) {
                        pw.println(entry.get(0).asText() + " / " + entry.get(1).asText());
                    }
                }
                pw.println("```");
            }

            // Table header
            pw.println("|id|n|mean|p50|p90|p95|p99|std_dev|");
            pw.println("|----:|----:|----:|----:|----:|----:|----:|----:|");

            // Table rows
            if (benchmarks.isArray()) {
                for (JsonNode bm : benchmarks) {
                    String id = bm.path("id").asText("");
                    String n = nf.format(Math.round(bm.path("n").asDouble(0)));
                    String mean = nf.format(Math.round(bm.path("mean").asDouble(0)));
                    String p50 = nf.format(Math.round(bm.path("p50").asDouble(0)));
                    String p90 = nf.format(Math.round(bm.path("p90").asDouble(0)));
                    String p95 = nf.format(Math.round(bm.path("p95").asDouble(0)));
                    String p99 = nf.format(Math.round(bm.path("p99").asDouble(0)));
                    String stdDev = nf.format(Math.round(bm.path("std_dev").asDouble(0)));
                    pw.println("|" + id
                               + "|" + n
                               + "|" + mean
                               + "|" + p50
                               + "|" + p90
                               + "|" + p95
                               + "|" + p99
                               + "|" + stdDev + "|");
                }
            }
        }
    }

    /**
     * Main entry point for command-line usage:
     * 
     * <pre>
     *   java -cp benchmarks.jar software.amazon.awssdk.benchmark.serde.JmhResultConverter &lt;input.json&gt; &lt;output-prefix&gt;
     * </pre>
     *
     * <p>Produces {@code <output-prefix>.json} and {@code <output-prefix>.md}.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            log.error(() -> "Usage: JmhResultConverter <input.json> <output-prefix>");
            throw new IllegalArgumentException("Expected 2 arguments: <input.json> <output-prefix>");
        }
        convert(args[0], args[1]);
    }
}

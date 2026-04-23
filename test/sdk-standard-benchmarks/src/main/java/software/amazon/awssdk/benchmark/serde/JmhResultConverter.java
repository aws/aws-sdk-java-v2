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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.utils.JavaSystemSetting;
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

    /**
     * Protocol prefixes that may appear on test case IDs. These must be stripped
     * before deduplication because the same test case may have been run with an
     * incorrect prefix due to a bug in earlier benchmark configurations.
     */
    private static final List<String> PROTOCOL_PREFIXES = Arrays.asList(
        "awsJson1_0_", "awsQuery_", "rpcv2Cbor_", "restJson1_", "restXml_"
    );

    /**
     * Pattern to extract the protocol portion from a benchmark class simple name.
     * E.g. "JsonRpc10MarshallBenchmark" &rarr; group(1) = "JsonRpc10".
     */
    private static final Pattern BENCHMARK_CLASS_PROTOCOL_PATTERN =
        Pattern.compile("^(.*?)(?:Marshall|Unmarshall)Benchmark$");

    /**
     * Maps the protocol name extracted from the benchmark class to the correct
     * prefix to use in JSON output IDs.
     */
    private static final Map<String, String> PROTOCOL_TO_PREFIX = new LinkedHashMap<String, String>();

    static {
        PROTOCOL_TO_PREFIX.put("JsonRpc10", "awsJson1_0_");
        PROTOCOL_TO_PREFIX.put("Query", "awsQuery_");
        PROTOCOL_TO_PREFIX.put("RestJson", "restJson1_");
        PROTOCOL_TO_PREFIX.put("RestXml", "restXml_");
        PROTOCOL_TO_PREFIX.put("RpcV2Cbor", "rpcv2Cbor_");
    }

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

        String os = JavaSystemSetting.OS_NAME.getStringValue().orElse("unknown") + " "
                + JavaSystemSetting.OS_VERSION.getStringValue().orElse("") + " "
                + JavaSystemSetting.OS_ARCH.getStringValue().orElse("");
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

        // Use a dedup key of (protocol + strippedId) to keep only the first occurrence.
        // This handles the case where the same test was run with different (wrong) prefixes.
        Map<String, ObjectNode> seen = new LinkedHashMap<String, ObjectNode>();

        for (JsonNode result : jmhResults) {
            String testCaseId = extractTestCaseId(result);
            if (testCaseId == null) {
                continue;
            }

            String protocol = extractProtocol(result);
            String strippedId = stripProtocolPrefix(testCaseId);
            String dedupKey = protocol + ":" + strippedId;

            if (seen.containsKey(dedupKey)) {
                log.debug(() -> "Skipping duplicate test case: " + testCaseId
                                + " (protocol=" + protocol + ", stripped=" + strippedId + ")");
                continue;
            }

            ObjectNode entry = convertSingleResult(result, strippedId, protocol);
            if (entry != null) {
                seen.put(dedupKey, entry);
            }
        }

        for (ObjectNode entry : seen.values()) {
            entries.add(entry);
        }
        return entries;
    }

    private static ObjectNode convertSingleResult(JsonNode result, String strippedId, String protocol) {
        long n = computeTotalInvocations(result.path("primaryMetric").path("rawDataHistogram"));
        JsonNode primaryMetric = result.path("primaryMetric");
        double mean = primaryMetric.path("score").asDouble(0.0);
        double stdDev = primaryMetric.path("scoreError").asDouble(0.0);

        JsonNode percentiles = primaryMetric.path("scorePercentiles");
        double p50 = percentiles.path("50.0").asDouble(0.0);
        double p90 = percentiles.path("90.0").asDouble(0.0);
        double p95 = percentiles.path("95.0").asDouble(0.0);
        double p99 = percentiles.path("99.0").asDouble(0.0);

        // Re-add the correct protocol prefix for the JSON id field
        String correctPrefix = PROTOCOL_TO_PREFIX.get(protocol);
        String prefixedId = (correctPrefix != null ? correctPrefix : "") + strippedId;

        ObjectNode entry = MAPPER.createObjectNode();
        entry.put("id", prefixedId);
        entry.put("protocol", protocol);
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
     * Strip any known protocol prefix from a test case ID.
     * For example, {@code "awsJson1_0_PutItemRequest_Baseline"} becomes
     * {@code "PutItemRequest_Baseline"}.
     */
    static String stripProtocolPrefix(String testCaseId) {
        for (String prefix : PROTOCOL_PREFIXES) {
            if (testCaseId.startsWith(prefix)) {
                return testCaseId.substring(prefix.length());
            }
        }
        return testCaseId;
    }

    /**
     * Extract the protocol name from the JMH result's {@code benchmark} field.
     *
     * <p>The benchmark field has the form
     * {@code "software.amazon.awssdk.benchmark.serde.JsonRpc10MarshallBenchmark.marshall"}.
     * This method extracts the simple class name and strips the {@code Marshall/UnmarshallBenchmark}
     * suffix to yield the protocol, e.g. {@code "JsonRpc10"}.
     *
     * @return the protocol name, or {@code "Unknown"} if it cannot be determined
     */
    static String extractProtocol(JsonNode result) {
        JsonNode benchmarkNode = result.path("benchmark");
        if (benchmarkNode.isMissingNode() || !benchmarkNode.isTextual()) {
            return "Unknown";
        }
        String benchmark = benchmarkNode.asText();

        // Extract simple class name: last segment before the method name
        // e.g. "...serde.JsonRpc10MarshallBenchmark.marshall" -> "JsonRpc10MarshallBenchmark"
        int lastDot = benchmark.lastIndexOf('.');
        if (lastDot < 0) {
            return "Unknown";
        }
        String withMethod = benchmark.substring(0, lastDot);
        int classNameStart = withMethod.lastIndexOf('.');
        String simpleClassName = classNameStart >= 0 ? withMethod.substring(classNameStart + 1) : withMethod;

        Matcher matcher = BENCHMARK_CLASS_PROTOCOL_PATTERN.matcher(simpleClassName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "Unknown";
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
     * |id|protocol|n|mean|p50|p90|p95|p99|std_dev|
     * |----:|----:|----:|----:|----:|----:|----:|----:|----:|
     * |PutItemRequest_Baseline|JsonRpc10|1,234|5,678|...|
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
            pw.println("|id|protocol|n|mean|p50|p90|p95|p99|std_dev|");
            pw.println("|----:|----:|----:|----:|----:|----:|----:|----:|----:|");

            // Table rows — use stripped (unprefixed) ID and show protocol in its own column
            if (benchmarks.isArray()) {
                for (JsonNode bm : benchmarks) {
                    String id = stripProtocolPrefix(bm.path("id").asText(""));
                    String protocol = bm.path("protocol").asText("");
                    String n = nf.format(Math.round(bm.path("n").asDouble(0)));
                    String mean = nf.format(Math.round(bm.path("mean").asDouble(0)));
                    String p50 = nf.format(Math.round(bm.path("p50").asDouble(0)));
                    String p90 = nf.format(Math.round(bm.path("p90").asDouble(0)));
                    String p95 = nf.format(Math.round(bm.path("p95").asDouble(0)));
                    String p99 = nf.format(Math.round(bm.path("p99").asDouble(0)));
                    String stdDev = nf.format(Math.round(bm.path("std_dev").asDouble(0)));
                    pw.println("|" + id
                               + "|" + protocol
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
     *   java -cp benchmarks.jar \
     *     software.amazon.awssdk.benchmark.serde.JmhResultConverter &lt;input.json&gt; &lt;output-prefix&gt;
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

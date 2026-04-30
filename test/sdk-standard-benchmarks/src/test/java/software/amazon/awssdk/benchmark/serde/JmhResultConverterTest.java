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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JmhResultConverterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void stripProtocolPrefix_removesKnownPrefixes() {
        assertThat(JmhResultConverter.stripProtocolPrefix("awsJson1_0_PutItemRequest_Baseline"))
            .isEqualTo("PutItemRequest_Baseline");
        assertThat(JmhResultConverter.stripProtocolPrefix("awsQuery_PutItemRequest_Baseline"))
            .isEqualTo("PutItemRequest_Baseline");
        assertThat(JmhResultConverter.stripProtocolPrefix("rpcv2Cbor_PutItemRequest_Baseline"))
            .isEqualTo("PutItemRequest_Baseline");
        assertThat(JmhResultConverter.stripProtocolPrefix("restJson1_PutItemRequest_Baseline"))
            .isEqualTo("PutItemRequest_Baseline");
        assertThat(JmhResultConverter.stripProtocolPrefix("restXml_PutItemRequest_Baseline"))
            .isEqualTo("PutItemRequest_Baseline");
    }

    @Test
    public void stripProtocolPrefix_leavesUnprefixedIdsUnchanged() {
        assertThat(JmhResultConverter.stripProtocolPrefix("PutItemRequest_Baseline"))
            .isEqualTo("PutItemRequest_Baseline");
    }

    @Test
    public void extractProtocol_fromMarshallBenchmark() {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("benchmark", "software.amazon.awssdk.benchmark.serde.JsonRpc10MarshallBenchmark.marshall");
        assertThat(JmhResultConverter.extractProtocol(result)).isEqualTo("JsonRpc10");
    }

    @Test
    public void extractProtocol_fromUnmarshallBenchmark() {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("benchmark", "software.amazon.awssdk.benchmark.serde.QueryUnmarshallBenchmark.unmarshall");
        assertThat(JmhResultConverter.extractProtocol(result)).isEqualTo("Query");
    }

    @Test
    public void extractProtocol_allProtocols() {
        assertProtocol("RestJsonMarshallBenchmark.marshall", "RestJson");
        assertProtocol("RestXmlUnmarshallBenchmark.unmarshall", "RestXml");
        assertProtocol("RpcV2CborMarshallBenchmark.marshall", "RpcV2Cbor");
    }

    @Test
    public void extractProtocol_missingBenchmarkField_returnsUnknown() {
        ObjectNode result = MAPPER.createObjectNode();
        assertThat(JmhResultConverter.extractProtocol(result)).isEqualTo("Unknown");
    }

    @Test
    public void buildOutput_deduplicatesSameTestCaseWithDifferentPrefixes() throws IOException {
        // Simulate two JMH results from JsonRpc10MarshallBenchmark with different prefixes
        // on the same underlying test case — these should be deduplicated to one entry.
        ArrayNode jmhResults = MAPPER.createArrayNode();
        jmhResults.add(createJmhResult(
            "software.amazon.awssdk.benchmark.serde.JsonRpc10MarshallBenchmark.marshall",
            "awsJson1_0_PutItemRequest_Baseline", 100.0));
        jmhResults.add(createJmhResult(
            "software.amazon.awssdk.benchmark.serde.JsonRpc10MarshallBenchmark.marshall",
            "rpcv2Cbor_PutItemRequest_Baseline", 200.0));

        ObjectNode output = JmhResultConverter.buildOutput(jmhResults);
        JsonNode benchmarks = output.path("serde_benchmarks");

        assertThat(benchmarks.isArray()).isTrue();
        assertThat(benchmarks.size()).isEqualTo(1);

        // The kept entry should have the correct prefix for JsonRpc10
        JsonNode entry = benchmarks.get(0);
        assertThat(entry.path("id").asText()).isEqualTo("awsJson1_0_PutItemRequest_Baseline");
        assertThat(entry.path("protocol").asText()).isEqualTo("JsonRpc10");
        // First occurrence wins
        assertThat(entry.path("mean").asDouble()).isEqualTo(100.0);
    }

    @Test
    public void buildOutput_keepsDistinctTestCases() throws IOException {
        ArrayNode jmhResults = MAPPER.createArrayNode();
        jmhResults.add(createJmhResult(
            "software.amazon.awssdk.benchmark.serde.JsonRpc10MarshallBenchmark.marshall",
            "awsJson1_0_PutItemRequest_Baseline", 100.0));
        jmhResults.add(createJmhResult(
            "software.amazon.awssdk.benchmark.serde.JsonRpc10MarshallBenchmark.marshall",
            "awsJson1_0_GetItemRequest_Baseline", 200.0));

        ObjectNode output = JmhResultConverter.buildOutput(jmhResults);
        JsonNode benchmarks = output.path("serde_benchmarks");

        assertThat(benchmarks.size()).isEqualTo(2);
        assertThat(benchmarks.get(0).path("id").asText()).isEqualTo("awsJson1_0_PutItemRequest_Baseline");
        assertThat(benchmarks.get(1).path("id").asText()).isEqualTo("awsJson1_0_GetItemRequest_Baseline");
    }

    @Test
    public void buildOutput_correctPrefixForEachProtocol() throws IOException {
        ArrayNode jmhResults = MAPPER.createArrayNode();
        jmhResults.add(createJmhResult(
            "software.amazon.awssdk.benchmark.serde.QueryMarshallBenchmark.marshall",
            "awsJson1_0_PutItemRequest_Baseline", 100.0));

        ObjectNode output = JmhResultConverter.buildOutput(jmhResults);
        JsonNode entry = output.path("serde_benchmarks").get(0);

        // Even though the original prefix was awsJson1_0_, the correct prefix for Query is awsQuery_
        assertThat(entry.path("id").asText()).isEqualTo("awsQuery_PutItemRequest_Baseline");
        assertThat(entry.path("protocol").asText()).isEqualTo("Query");
    }

    @Test
    public void writeMarkdown_containsProtocolColumn(@TempDir File tempDir) throws IOException {
        ArrayNode jmhResults = MAPPER.createArrayNode();
        jmhResults.add(createJmhResult(
            "software.amazon.awssdk.benchmark.serde.JsonRpc10MarshallBenchmark.marshall",
            "awsJson1_0_PutItemRequest_Baseline", 1234.0));

        ObjectNode output = JmhResultConverter.buildOutput(jmhResults);
        File mdFile = new File(tempDir, "test.md");
        JmhResultConverter.writeMarkdown(output, mdFile);

        String content = new String(Files.readAllBytes(mdFile.toPath()), StandardCharsets.UTF_8);

        // Header should include protocol column
        assertThat(content).contains("|id|protocol|n|mean|p50|p90|p95|p99|std_dev|");
        // Row should have stripped ID (no prefix) and protocol
        assertThat(content).contains("|PutItemRequest_Baseline|JsonRpc10|");
    }

    private void assertProtocol(String benchmarkSuffix, String expectedProtocol) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("benchmark", "software.amazon.awssdk.benchmark.serde." + benchmarkSuffix);
        assertThat(JmhResultConverter.extractProtocol(result)).isEqualTo(expectedProtocol);
    }

    private ObjectNode createJmhResult(String benchmark, String testCaseId, double score) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("benchmark", benchmark);

        ObjectNode params = MAPPER.createObjectNode();
        params.put("testCaseId", testCaseId);
        result.set("params", params);

        ObjectNode primaryMetric = MAPPER.createObjectNode();
        primaryMetric.put("score", score);
        primaryMetric.put("scoreError", 10.0);

        ObjectNode percentiles = MAPPER.createObjectNode();
        percentiles.put("50.0", score * 0.9);
        percentiles.put("90.0", score * 1.1);
        percentiles.put("95.0", score * 1.2);
        percentiles.put("99.0", score * 1.5);
        primaryMetric.set("scorePercentiles", percentiles);

        // Minimal rawDataHistogram: [[[value, count]]]
        ArrayNode histogram = MAPPER.createArrayNode();
        ArrayNode fork = MAPPER.createArrayNode();
        ArrayNode iteration = MAPPER.createArrayNode();
        ArrayNode bin = MAPPER.createArrayNode();
        bin.add(score);
        bin.add(1000);
        iteration.add(bin);
        fork.add(iteration);
        histogram.add(fork);
        primaryMetric.set("rawDataHistogram", histogram);

        result.set("primaryMetric", primaryMetric);
        return result;
    }
}

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

package software.amazon.awssdk.s3benchmarks.s3express;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class S3BenchmarkRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void outputJson_hasExpectedStructure() throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(buildResult("s3express.S3Benchmark.putObject", 12.5, "ms/op", "s3express", "64KB", "Apache"));
        results.add(buildResult("s3express.S3Benchmark.readThroughput", 96000000.0, "bytes/s", "s3express", "64KB", "Apache"));

        Path outputFile = tempDir.resolve("results.json");
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outputFile.toFile(), results);

        JsonNode root = new ObjectMapper().readTree(outputFile.toFile());
        assertThat(root.isArray()).isTrue();
        assertThat(root.size()).isEqualTo(2);

        JsonNode first = root.get(0);
        assertThat(first.get("benchmark").asText()).isEqualTo("s3express.S3Benchmark.putObject");
        assertThat(first.get("mode").asText()).isEqualTo("avgt");
        assertThat(first.get("params").get("bucketType").asText()).isEqualTo("s3express");
        assertThat(first.get("params").get("objectSize").asText()).isEqualTo("64KB");
        assertThat(first.get("params").get("httpClient").asText()).isEqualTo("Apache");
        assertThat(first.get("primaryMetric").get("score").asDouble()).isEqualTo(12.5);
        assertThat(first.get("primaryMetric").get("scoreUnit").asText()).isEqualTo("ms/op");
        assertThat(first.get("primaryMetric").get("rawData").get(0).get(0).asDouble()).isEqualTo(12.5);

        JsonNode second = root.get(1);
        assertThat(second.get("benchmark").asText()).isEqualTo("s3express.S3Benchmark.readThroughput");
        assertThat(second.get("primaryMetric").get("scoreUnit").asText()).isEqualTo("bytes/s");
    }

    private static Map<String, Object> buildResult(String benchmarkName, double score, String scoreUnit,
                                                   String bucketType, String objectSize, String httpClient) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("benchmark", benchmarkName);
        result.put("mode", "avgt");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("bucketType", bucketType);
        params.put("objectSize", objectSize);
        params.put("httpClient", httpClient);
        result.put("params", params);

        Map<String, Object> primaryMetric = new LinkedHashMap<>();
        primaryMetric.put("score", score);
        primaryMetric.put("scoreUnit", scoreUnit);

        List<List<Double>> rawData = new ArrayList<>();
        List<Double> fork = new ArrayList<>();
        fork.add(score);
        rawData.add(fork);
        primaryMetric.put("rawData", rawData);

        result.put("primaryMetric", primaryMetric);
        return result;
    }
}

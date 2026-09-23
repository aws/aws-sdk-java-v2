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

package software.amazon.awssdk.codegen.customization.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;

class DefaultReadWriteTimeoutExemptionProcessorTest {

    private static final String EXEMPTIONS_RESOURCE = "software/amazon/awssdk/codegen/default-read-write-timeout-exemptions.json";
    private static final Pattern SERVICE_ID = Pattern.compile("\"serviceId\"\\s*:\\s*\"([^\"]+)\"");

    @ParameterizedTest
    @MethodSource("exemptionEntries")
    void postprocess_serviceInArtifact_bakesExpectedTier(String serviceId, long expectedMillis) {
        IntermediateModel model = modelWithServiceId(serviceId);

        new DefaultReadWriteTimeoutExemptionProcessor().postprocess(model);

        assertThat(model.getMetadata().getDefaultReadWriteTimeoutMillis()).isEqualTo(expectedMillis);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sqs", "s3", "lambda", "kinesis", "CODEARTIFACT", "MGN", "Sqs"})
    void postprocess_misCasedServiceId_bakesNothing(String misCasedServiceId) {
        IntermediateModel model = modelWithServiceId(misCasedServiceId);

        new DefaultReadWriteTimeoutExemptionProcessor().postprocess(model);

        assertThat(model.getMetadata().getDefaultReadWriteTimeoutMillis()).isNull();
    }

    @Test
    void postprocess_serviceNotInArtifact_bakesNothing() {
        IntermediateModel model = modelWithServiceId("Not A Real Service");

        new DefaultReadWriteTimeoutExemptionProcessor().postprocess(model);

        assertThat(model.getMetadata().getDefaultReadWriteTimeoutMillis()).isNull();
    }

    @Test
    void validateArtifactKeys_everyKeyMatchesARealServiceId() throws IOException {
        new DefaultReadWriteTimeoutExemptionProcessor().validateArtifactKeys(realServiceIds());
    }

    @Test
    void validateArtifactKeys_keyMatchingNoServiceId_throws() {
        DefaultReadWriteTimeoutExemptionProcessor processor =
            new DefaultReadWriteTimeoutExemptionProcessor(Collections.singletonMap("sqs", 900000L));

        assertThatThrownBy(() -> processor.validateArtifactKeys(Collections.singleton("SQS")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("sqs");
    }

    private static IntermediateModel modelWithServiceId(String serviceId) {
        IntermediateModel model = new IntermediateModel();
        model.setMetadata(new Metadata().withServiceId(serviceId));
        return model;
    }

    private static Stream<Arguments> exemptionEntries() throws IOException {
        try (InputStream stream = DefaultReadWriteTimeoutExemptionProcessorTest.class.getClassLoader()
                                                                                     .getResourceAsStream(EXEMPTIONS_RESOURCE)) {
            Map<String, JsonNode> artifact = JsonNodeParser.create().parse(stream).asObject();
            return artifact.entrySet().stream()
                           .map(e -> Arguments.of(e.getKey(), Long.parseLong(e.getValue().asNumber())));
        }
    }

    private static Set<String> realServiceIds() throws IOException {
        Path servicesDir = locateServicesDir();
        Set<String> serviceIds = new HashSet<>();
        try (DirectoryStream<Path> modules = Files.newDirectoryStream(servicesDir)) {
            for (Path module : modules) {
                Path model = module.resolve("src/main/resources/codegen-resources/service-2.json");
                if (Files.isRegularFile(model)) {
                    extractServiceId(model).ifPresent(serviceIds::add);
                }
            }
        }
        assertThat(serviceIds).as("expected to harvest serviceIds from the service models").isNotEmpty();
        return serviceIds;
    }

    private static Optional<String> extractServiceId(Path serviceModel) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(serviceModel, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = SERVICE_ID.matcher(line);
                if (matcher.find()) {
                    return Optional.of(matcher.group(1));
                }
            }
        }
        return Optional.empty();
    }

    private static Path locateServicesDir() {
        for (Path candidate : new Path[] {Paths.get("..", "services"), Paths.get("services"), Paths.get("..", "..", "services")}) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not locate the services/ directory from " + Paths.get("").toAbsolutePath());
    }
}

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.Validate;

/**
 * Bakes the per-service default read/write inactivity timeout tier into the generated service HTTP config. The tiers come from a
 * checked-in copy of the shared exemption artifact ({@code default-read-write-timeout-exemptions.json}), keyed by the service's
 * sdkId ({@link software.amazon.awssdk.codegen.model.intermediate.Metadata#getServiceId()}).
 *
 * <p>An artifact value of {@code -1} marks a fully-exempt service (no default timeout applies); a positive value is the applied
 * timeout in milliseconds. A service absent from the artifact has nothing baked, and {@code aws-core} supplies the flat default
 * when the rollout gate is on. The rollout gate itself is applied later, in {@code aws-core}; this processor only bakes the
 * per-service tier, which is the same regardless of whether the gate is on.
 */
public class DefaultReadWriteTimeoutExemptionProcessor implements CodegenCustomizationProcessor {

    private static final String EXEMPTIONS_RESOURCE = "software/amazon/awssdk/codegen/default-read-write-timeout-exemptions.json";

    private static final Map<String, Long> SERVICE_ID_TO_TIMEOUT_MILLIS = loadExemptions();

    private final Map<String, Long> serviceIdToTimeoutMillis;

    public DefaultReadWriteTimeoutExemptionProcessor() {
        this(SERVICE_ID_TO_TIMEOUT_MILLIS);
    }

    @SdkTestInternalApi
    DefaultReadWriteTimeoutExemptionProcessor(Map<String, Long> serviceIdToTimeoutMillis) {
        this.serviceIdToTimeoutMillis = serviceIdToTimeoutMillis;
    }

    @Override
    public void preprocess(ServiceModel serviceModel) {
        // no-op
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        String serviceId = intermediateModel.getMetadata().getServiceId();
        Long timeoutMillis = serviceIdToTimeoutMillis.get(serviceId);
        if (timeoutMillis != null) {
            intermediateModel.getMetadata().setDefaultReadWriteTimeoutMillis(timeoutMillis);
        }
    }

    /**
     * Fails if any artifact key does not match one of {@code knownServiceIds}. Matching is exact (case-sensitive), so a stale
     * key (no such service) or a mis-cased key both surface here: either would otherwise silently leave the intended service
     * unlisted and wrongly apply the flat default instead of its exempt/partial tier.
     *
     * <p>Codegen processes one service per run, so this whole-artifact cross-check cannot run inside {@link #postprocess} (a
     * single run never sees every serviceId). It is invoked at build time by the coverage test against the full set of service
     * sdkIds.
     */
    void validateArtifactKeys(Set<String> knownServiceIds) {
        List<String> unknownKeys = serviceIdToTimeoutMillis.keySet().stream()
                                                           .filter(key -> !knownServiceIds.contains(key))
                                                           .sorted()
                                                           .collect(Collectors.toList());
        if (!unknownKeys.isEmpty()) {
            throw new IllegalStateException(
                "Read/write timeout exemption artifact " + EXEMPTIONS_RESOURCE + " contains key(s) matching no service sdkId "
                + "(a stale or mis-cased key silently leaves that service unlisted): " + unknownKeys);
        }
    }

    private static Map<String, Long> loadExemptions() {
        Map<String, Long> exemptions = new HashMap<>();
        try (InputStream stream = DefaultReadWriteTimeoutExemptionProcessor.class.getClassLoader()
                                                                                 .getResourceAsStream(EXEMPTIONS_RESOURCE)) {
            Validate.notNull(stream, "Failed to load read/write timeout exemption artifact: %s", EXEMPTIONS_RESOURCE);
            JsonNode root = JsonNodeParser.create().parse(stream);
            root.asObject().forEach((serviceId, value) -> exemptions.put(serviceId, parseTimeoutMillis(serviceId, value)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read read/write timeout exemption artifact: " + EXEMPTIONS_RESOURCE, e);
        }
        return Collections.unmodifiableMap(exemptions);
    }

    private static long parseTimeoutMillis(String serviceId, JsonNode value) {
        try {
            return Long.parseLong(value.asNumber());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                "Invalid numeric value for key '" + serviceId + "' in " + EXEMPTIONS_RESOURCE + ": " + value, e);
        }
    }
}

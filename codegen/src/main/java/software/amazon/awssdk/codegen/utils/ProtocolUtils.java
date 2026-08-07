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

package software.amazon.awssdk.codegen.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Resolves the protocol from the service model {@code protocol} and {@code protocols} fields.
 */
public final class ProtocolUtils {

    /**
     * Priority-ordered list of protocols supported by the SDK.
     */
    private static final List<String> SUPPORTED_PROTOCOLS = Arrays.asList(
        "smithy-rpc-v2-cbor", "json", "rest-json", "rest-xml", "query", "ec2");

    /**
     * Maps a Smithy protocol-trait shape id to the SDK protocol string. {@code awsJson1_0} and
     * {@code awsJson1_1} both map to {@code json}; the JSON version is resolved separately at the
     * metadata layer.
     */
    private static final Map<String, String> SMITHY_PROTOCOL_TRAITS = new HashMap<>();

    static {
        SMITHY_PROTOCOL_TRAITS.put("smithy.protocols#rpcv2Cbor", "smithy-rpc-v2-cbor");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#awsJson1_0", "json");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#awsJson1_1", "json");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#restJson1", "rest-json");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#restXml", "rest-xml");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#awsQuery", "query");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#ec2Query", "ec2");
    }

    private ProtocolUtils() {
    }

    /**
     * {@code protocols} supersedes {@code protocol}. The highest priority protocol supported by the SDK that is present in the
     * service model {@code protocols} list will be selected. If none of the values in {@code protocols} is supported by the
     * SDK, an error will be thrown. If {@code protocols} is empty or null, the value from {@code protocol} will be returned.
     */
    public static String resolveProtocol(ServiceMetadata serviceMetadata) {

        List<String> protocols = serviceMetadata.getProtocols();
        String protocol = serviceMetadata.getProtocol();

        if (protocols == null || protocols.isEmpty()) {
            return protocol;
        }

        // Kinesis uses customization.config customServiceMetadata to set cbor
        if ("cbor".equals(protocols.get(0))) {
            return "cbor";
        }

        for (String supportedProtocol : SUPPORTED_PROTOCOLS) {
            if (protocols.contains(supportedProtocol)) {
                return supportedProtocol;
            }
        }

        throw new IllegalArgumentException("The SDK does not support any of provided protocols: " + protocols);
    }

    /**
     * Smithy equivalent of {@link #resolveProtocol(ServiceMetadata)}: maps the service's applied
     * protocol traits to SDK strings and picks the highest priority from {@link #SUPPORTED_PROTOCOLS}.
     * A service may apply several (e.g. SQS has both {@code awsQuery} and {@code awsJson1_0}); the
     * priority order matches the C2J {@code protocols}-list resolution.
     *
     * @throws IllegalArgumentException if no applied protocol trait is supported.
     */
    public static String resolveProtocol(ServiceIndex serviceIndex, ServiceShape service) {
        Map<ShapeId, ?> protocolTraits = serviceIndex.getProtocols(service);

        List<String> resolved = new ArrayList<>();
        for (ShapeId protocolTraitId : protocolTraits.keySet()) {
            String sdkProtocol = SMITHY_PROTOCOL_TRAITS.get(protocolTraitId.toString());
            if (sdkProtocol != null) {
                resolved.add(sdkProtocol);
            }
        }

        for (String supportedProtocol : SUPPORTED_PROTOCOLS) {
            if (resolved.contains(supportedProtocol)) {
                return supportedProtocol;
            }
        }

        throw new IllegalArgumentException(
            "The SDK does not support any of the protocols applied to service " + service.getId()
            + ": " + protocolTraits.keySet());
    }
}

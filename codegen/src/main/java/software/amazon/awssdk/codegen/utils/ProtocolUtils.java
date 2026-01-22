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
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Resolves the protocol from the service model {@code protocol} and {@code protocols} fields.
 */
public final class ProtocolUtils {

    /**
     * Priority-ordered list of protocols supported by the SDK.
     */
    private static final List<String> SUPPORTED_PROTOCOLS = Arrays.asList(
        "smithy-rpc-v2-cbor", "json", "rest-json", "rest-xml", "query", "ec2");
    private static final Logger log = LoggerFactory.getLogger(ProtocolUtils.class);

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

    public static String resolveProtocol(ServiceIndex serviceIndex, ServiceShape service) {
        List<String> protocols = new ArrayList<>();
        for (Map.Entry<ShapeId, Trait> entry : serviceIndex.getProtocols(service).entrySet()) {
            // TODO: Map traits for all protocols
            // "smithy-rpc-v2-cbor", "json", "rest-json", "rest-xml", "query", "ec2"
            switch(entry.getKey().getName()) {
                case "restJson1":
                    protocols.add("rest-json");
                    break;
                default:
                    // TODO: Should this be a debug log?
                    throw new IllegalArgumentException("Unable to translate protocol trait");
            }
        }

        for (String supportedProtocol : SUPPORTED_PROTOCOLS) {
            if (protocols.contains(supportedProtocol)) {
                return supportedProtocol;
            }
        }

        throw new IllegalArgumentException("The SDK does not support any of provided protocols: " + protocols);
    }
}

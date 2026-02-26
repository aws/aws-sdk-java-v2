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

package software.amazon.awssdk.codegen.smithy.customization.processors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.model.config.customization.MetadataConfig;
import software.amazon.awssdk.codegen.model.config.customization.SmithyMetadataConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.MetadataModifiersProcessor}.
 *
 * <p>In preprocess, applies the configured Smithy protocol trait to the service shape.
 * In postprocess, applies the contentType override to the IntermediateModel metadata.
 *
 * <p>Follows the dual-config pattern: accepts both old C2J {@link MetadataConfig} and new
 * Smithy-native {@link SmithyMetadataConfig}. Old C2J protocol strings (e.g., "rest-json")
 * are translated to Smithy protocol trait ShapeIds (e.g., "aws.protocols#restJson1").
 */
public class MetadataModifiersProcessor
        extends AbstractDualConfigProcessor<MetadataConfig, SmithyMetadataConfig> {

    /**
     * Mapping from C2J protocol strings to Smithy protocol trait ShapeId strings.
     */
    private static final Map<String, String> C2J_TO_SMITHY_PROTOCOL;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("rest-json", "aws.protocols#restJson1");
        map.put("json", "aws.protocols#awsJson1_1");
        map.put("rest-xml", "aws.protocols#restXml");
        map.put("query", "aws.protocols#awsQuery");
        map.put("ec2", "aws.protocols#ec2Query");
        map.put("cbor", "smithy.protocols#rpcv2Cbor");
        map.put("smithy-rpc-v2-cbor", "smithy.protocols#rpcv2Cbor");
        C2J_TO_SMITHY_PROTOCOL = Collections.unmodifiableMap(map);
    }

    private String contentType;

    public MetadataModifiersProcessor(MetadataConfig oldConfig, SmithyMetadataConfig newConfig) {
        super(oldConfig, newConfig, "customServiceMetadata", "smithyCustomServiceMetadata");
    }

    @Override
    protected boolean isSet(Object config) {
        if (config instanceof MetadataConfig) {
            MetadataConfig mc = (MetadataConfig) config;
            return mc.getProtocol() != null || mc.getContentType() != null;
        }
        if (config instanceof SmithyMetadataConfig) {
            SmithyMetadataConfig smc = (SmithyMetadataConfig) config;
            return smc.getProtocol() != null || smc.getContentType() != null;
        }
        return config != null;
    }

    @Override
    protected SmithyMetadataConfig convertOldToNew(MetadataConfig oldConfig, Model model, ServiceShape service) {
        SmithyMetadataConfig smithyConfig = new SmithyMetadataConfig();

        String c2jProtocol = oldConfig.getProtocol();
        if (c2jProtocol != null) {
            String smithyProtocol = C2J_TO_SMITHY_PROTOCOL.get(c2jProtocol);
            if (smithyProtocol == null) {
                throw new IllegalStateException(
                    String.format("Unknown C2J protocol string '%s'. Cannot convert to Smithy protocol trait ShapeId. "
                                  + "Known protocols: %s", c2jProtocol, C2J_TO_SMITHY_PROTOCOL.keySet()));
            }
            smithyConfig.setProtocol(smithyProtocol);
        }

        smithyConfig.setContentType(oldConfig.getContentType());
        return smithyConfig;
    }

    @Override
    protected Model applySmithyLogic(Model model, ServiceShape service, SmithyMetadataConfig config) {
        this.contentType = config.getContentType();

        String protocol = config.getProtocol();
        if (protocol == null) {
            return model;
        }

        ShapeId protocolTraitId = ShapeId.from(protocol);
        DynamicTrait protocolTrait = new DynamicTrait(protocolTraitId, Node.objectNode());
        ServiceShape updatedService = service.toBuilder()
                                             .addTrait(protocolTrait)
                                             .build();
        return model.toBuilder()
                    .addShape(updatedService)
                    .build();
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        if (contentType != null) {
            intermediateModel.getMetadata().setContentType(contentType);
        }
    }
}

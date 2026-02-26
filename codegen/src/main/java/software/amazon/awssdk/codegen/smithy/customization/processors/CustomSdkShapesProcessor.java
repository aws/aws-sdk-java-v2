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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.config.customization.CustomSdkShapes;
import software.amazon.awssdk.codegen.model.config.customization.SmithyCustomSdkShapes;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.smithy.customization.ShapeIdResolver;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.CustomSdkShapesProcessor}.
 *
 * <p>Adds custom SDK shapes to the Smithy model. Supports both old C2J {@link CustomSdkShapes}
 * (which contains a {@code Map<String, Shape>} of C2J Shape objects) and new Smithy-native
 * {@link SmithyCustomSdkShapes} (which contains a Smithy JSON AST document).
 *
 * <p>For old config, each C2J Shape is converted to its Smithy JSON AST equivalent: types are
 * mapped directly, enum values become {@code smithy.api#enum} traits, and structure members
 * become member maps with fully-qualified ShapeId targets.
 *
 * <p>The JSON AST is then parsed using {@code ModelAssembler.addUnparsedModel()} and the
 * resulting non-prelude shapes are merged into the existing model.
 */
public class CustomSdkShapesProcessor
        extends AbstractDualConfigProcessor<CustomSdkShapes, SmithyCustomSdkShapes> {

    public CustomSdkShapesProcessor(CustomSdkShapes oldConfig, SmithyCustomSdkShapes newConfig) {
        super(oldConfig, newConfig, "customSdkShapes", "smithyCustomSdkShapes");
    }

    @Override
    protected boolean isSet(Object config) {
        if (config instanceof SmithyCustomSdkShapes) {
            Map<String, Object> ast = ((SmithyCustomSdkShapes) config).getSmithyAst();
            return ast != null && !ast.isEmpty();
        }
        if (config instanceof CustomSdkShapes) {
            CustomSdkShapes c2j = (CustomSdkShapes) config;
            return c2j.getShapes() != null && !c2j.getShapes().isEmpty();
        }
        return config != null;
    }

    /**
     * Converts old C2J CustomSdkShapes to a SmithyCustomSdkShapes containing
     * a Smithy JSON AST document. Each C2J Shape is translated to its JSON AST
     * equivalent within the service namespace.
     */
    @Override
    protected SmithyCustomSdkShapes convertOldToNew(CustomSdkShapes old, Model model, ServiceShape service) {
        String namespace = ShapeIdResolver.namespace(service);
        Map<String, Object> shapesMap = new LinkedHashMap<>();

        for (Map.Entry<String, Shape> entry : old.getShapes().entrySet()) {
            String simpleName = entry.getKey();
            Shape c2jShape = entry.getValue();
            String shapeId = namespace + "#" + simpleName;
            shapesMap.put(shapeId, convertC2jShapeToAst(c2jShape, namespace));
        }

        Map<String, Object> ast = new LinkedHashMap<>();
        ast.put("smithy", "2.0");
        ast.put("shapes", shapesMap);

        SmithyCustomSdkShapes result = new SmithyCustomSdkShapes();
        result.setSmithyAst(ast);
        return result;
    }

    /**
     * Converts a single C2J Shape object to a Smithy JSON AST shape definition.
     * Handles the shape types actually used in customSdkShapes configs:
     * string (with optional enum), integer, structure.
     */
    private Map<String, Object> convertC2jShapeToAst(Shape c2jShape, String namespace) {
        Map<String, Object> astShape = new LinkedHashMap<>();
        String c2jType = c2jShape.getType();

        // Map C2J type to Smithy type — they use the same names
        astShape.put("type", c2jType);

        // Handle enum values → smithy.api#enum trait
        if (c2jShape.getEnumValues() != null && !c2jShape.getEnumValues().isEmpty()) {
            Map<String, Object> traits = new LinkedHashMap<>();
            List<Map<String, String>> enumDefs = new ArrayList<>();
            for (String enumValue : c2jShape.getEnumValues()) {
                Map<String, String> enumDef = new LinkedHashMap<>();
                enumDef.put("value", enumValue);
                enumDefs.add(enumDef);
            }
            traits.put("smithy.api#enum", enumDefs);
            astShape.put("traits", traits);
        }

        // Handle structure members
        if (c2jShape.getMembers() != null && !c2jShape.getMembers().isEmpty()) {
            Map<String, Object> members = new LinkedHashMap<>();
            for (Map.Entry<String, Member> memberEntry : c2jShape.getMembers().entrySet()) {
                Map<String, Object> memberDef = new LinkedHashMap<>();
                String targetName = memberEntry.getValue().getShape();
                memberDef.put("target", namespace + "#" + targetName);
                members.put(memberEntry.getKey(), memberDef);
            }
            astShape.put("members", members);
        }

        return astShape;
    }

    /**
     * Parses the Smithy JSON AST document using ModelAssembler and merges
     * the resulting non-prelude shapes into the existing model.
     */
    @Override
    protected Model applySmithyLogic(Model model, ServiceShape service, SmithyCustomSdkShapes config) {
        Map<String, Object> ast = config.getSmithyAst();
        if (ast == null || ast.isEmpty()) {
            return model;
        }

        // Serialize the AST map to a JSON string via Smithy's Node system
        NodeMapper nodeMapper = new NodeMapper();
        String jsonString = Node.printJson(nodeMapper.serialize(ast));

        // Parse the JSON AST using Smithy's ModelAssembler
        Model customShapesModel = Model.assembler()
            .addUnparsedModel("customSdkShapes.json", jsonString)
            .assemble()
            .unwrap();

        // Merge custom shapes into the existing model, excluding prelude shapes
        Model.Builder builder = model.toBuilder();
        for (software.amazon.smithy.model.shapes.Shape shape : customShapesModel.toSet()) {
            if (!Prelude.isPreludeShape(shape)) {
                builder.addShape(shape);
            }
        }

        return builder.build();
    }
}

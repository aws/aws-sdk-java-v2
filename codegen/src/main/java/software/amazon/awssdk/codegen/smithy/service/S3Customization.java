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

package software.amazon.awssdk.codegen.smithy.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.ModifyModelShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.ShapeModifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ParameterHttpMapping;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.CustomOperationContextParam;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * The S3 differences from C2J that a validated smithy-build transform cannot express:
 * <ul>
 *     <li>Members {@code customization.config} injects with bindings Smithy rejects: {@code ExpiresString} shares the
 *     {@code Expires} header, and {@code SdkPartType} is unbound next to an {@code @httpPayload} member.</li>
 *     <li>{@code endpointParameters} and {@code customOperationContextParams}, applied to the intermediate model as on
 *     C2J.</li>
 *     <li>The part-number markers, integers in C2J and strings in Smithy, where Smithy requires a pagination token to be a
 *     string.</li>
 *     <li>The s3 variant of SigV4, and request URIs without the bucket label, which the endpoint rules place, and
 *     without {@code x-id}.</li>
 *     <li>Members {@code customization.config} renames, such as {@code CopyObjectRequest.Bucket}, which C2J leaves not
 *     required and, for a URI label, on the old location name and label.</li>
 * </ul>
 */
@SdkInternalApi
final class S3Customization implements SmithyServiceCustomization {
    private static final String NAMESPACE = "com.amazonaws.s3";
    private static final Set<String> INJECTED = new HashSet<>(Arrays.asList(
        "GetObjectOutput.ExpiresString", "HeadObjectOutput.ExpiresString", "UploadPartRequest.SdkPartType"));
    private static final List<String> INTEGER_SHAPES = Arrays.asList("PartNumberMarker", "NextPartNumberMarker");

    @Override
    public Set<String> sdkIds() {
        return Collections.singleton("S3");
    }

    @Override
    public boolean appliesConfig(String key) {
        return key.equals("endpointParameters") || key.equals("customOperationContextParams")
               || key.startsWith("shapeModifiers.") && key.contains(".inject.")
                  && INJECTED.contains(key.substring("shapeModifiers.".length()).replace(".inject.", "."))
               || key.startsWith("shapeModifiers.") && key.contains(".modify.") && key.endsWith(".required");
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        List<Shape> replaced = new ArrayList<>();
        Map<String, ShapeModifier> modifiers = config.getShapeModifiers() == null ? Collections.emptyMap()
                                                                                 : config.getShapeModifiers();
        for (String injected : INJECTED) {
            String shapeName = injected.substring(0, injected.indexOf('.'));
            String memberName = injected.substring(injected.indexOf('.') + 1);
            Member member = injectedMember(modifiers.get(shapeName), memberName);
            if (member == null) {
                continue;
            }
            StructureShape container = model.expectShape(ShapeId.fromParts(NAMESPACE, shapeName), StructureShape.class);
            MemberShape.Builder shape = MemberShape.builder()
                                                   .id(container.getId().withMember(memberName))
                                                   .target(ShapeId.fromParts(NAMESPACE, member.getShape()));
            if (member.getDocumentation() != null) {
                shape.addTrait(new DocumentationTrait(member.getDocumentation()));
            }
            if ("header".equals(member.getLocation())) {
                shape.addTrait(new HttpHeaderTrait(member.getLocationName()));
            }
            replaced.add(container.toBuilder().addMember(shape.build()).build());
        }
        for (String name : INTEGER_SHAPES) {
            Shape string = model.expectShape(ShapeId.fromParts(NAMESPACE, name));
            replaced.add(IntegerShape.builder().id(string.getId()).addTraits(string.getAllTraits().values()).build());
        }
        return ModelTransformer.create().replaceShapes(model, replaced);
    }

    private static Member injectedMember(ShapeModifier modifier, String memberName) {
        if (modifier == null || modifier.getInject() == null) {
            return null;
        }
        return modifier.getInject().stream()
                       .filter(m -> m.containsKey(memberName))
                       .findFirst()
                       .map(m -> m.get(memberName))
                       .orElse(null);
    }

    @Override
    public void postprocess(IntermediateModel model) {
        model.getMetadata().setAuthType(AuthType.S3);
        addEndpointParameters(model);
        addOperationContextParams(model);
        restoreRenamedMembers(model);
        for (ShapeModel shape : model.getShapes().values()) {
            if (shape.getMarshaller() != null && shape.getMarshaller().getRequestUri() != null) {
                shape.getMarshaller().setRequestUri(withoutXId(withoutBucket(shape)));
            }
        }
    }

    private static void addEndpointParameters(IntermediateModel model) {
        Map<String, software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel> parameters =
            model.getCustomizationConfig().getEndpointParameters();
        if (parameters == null) {
            return;
        }
        Map<String, software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel> merged =
            new LinkedHashMap<>(model.getEndpointParameters());
        parameters.forEach((name, parameter) -> {
            if (merged.containsKey(name)) {
                throw new IllegalStateException("Duplicate endpoint parameter " + name);
            }
            merged.put(name, parameter);
        });
        model.setEndpointParameters(merged);
    }

    private static void addOperationContextParams(IntermediateModel model) {
        List<CustomOperationContextParam> params = model.getCustomizationConfig().getCustomOperationContextParams();
        if (params == null) {
            return;
        }
        for (CustomOperationContextParam param : params) {
            OperationModel operation = model.getOperation(param.getOperationName());
            if (operation == null || operation.getOperationContextParams() != null) {
                throw new IllegalStateException("Cannot add operation context params to " + param.getOperationName());
            }
            operation.setOperationContextParams(param.getOperationContextParamsMap());
        }
    }

    /**
     * C2J's {@code ShapeModifiersProcessor} renames a member but leaves the shape's required list on the old name, so the
     * renamed member is not required, and keeps the old name as the location name, so a URI label keeps it too. The
     * {@code renameMembers} transform moves both to the new name; this moves them back.
     */
    private static void restoreRenamedMembers(IntermediateModel model) {
        Map<String, ShapeModifier> modifiers = model.getCustomizationConfig().getShapeModifiers();
        if (modifiers == null) {
            return;
        }
        modifiers.forEach((shapeName, modifier) -> {
            if (modifier.getModify() == null) {
                return;
            }
            for (Map<String, ModifyModelShapeModifier> modify : modifier.getModify()) {
                modify.forEach((oldName, value) -> {
                    if (value.getEmitPropertyName() != null) {
                        restoreRenamedMember(shape(model, shapeName), oldName, value.getEmitPropertyName());
                    }
                });
            }
        });
    }

    private static ShapeModel shape(IntermediateModel model, String c2jName) {
        return model.getShapes().values().stream()
                    .filter(s -> c2jName.equals(s.getC2jName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("shapeModifiers." + c2jName + ": no such shape"));
    }

    private static void restoreRenamedMember(ShapeModel shape, String oldName, String newName) {
        MemberModel member = shape.getMemberByC2jName(newName);
        if (member == null) {
            throw new IllegalStateException(shape.getC2jName() + " has no renamed member " + newName);
        }
        if (member.isRequired()) {
            member.setRequired(false);
            List<String> required = shape.getRequired();
            required.set(required.indexOf(newName), oldName);
        }
        ParameterHttpMapping http = member.getHttp();
        if (http != null && http.isUri()) {
            http.setMarshallLocationName(oldName);
            http.setUnmarshallLocationName(oldName);
            String uri = shape.getMarshaller().getRequestUri()
                              .replace("{" + newName + "}", "{" + oldName + "}")
                              .replace("{" + newName + "+}", "{" + oldName + "+}");
            shape.getMarshaller().setRequestUri(uri);
        }
    }

    /**
     * C2J's {@code S3RemoveBucketFromUriProcessor} edit: the bucket label removed once, found through its context
     * param so it follows the CopyObject and UploadPartCopy rename.
     */
    private static String withoutBucket(ShapeModel shape) {
        String uri = shape.getMarshaller().getRequestUri();
        if (shape.getMembers() == null) {
            return uri;
        }
        for (MemberModel member : shape.getMembers()) {
            if (member.getContextParam() != null && "Bucket".equals(member.getContextParam().getName())
                && member.getHttp() != null && member.getHttp().isUri()) {
                String label = "/{" + member.getHttp().getMarshallLocationName() + "}";
                int index = uri.indexOf(label);
                if (index >= 0) {
                    return uri.substring(0, index) + uri.substring(index + label.length());
                }
            }
        }
        return uri;
    }

    private static String withoutXId(String uri) {
        int queryStart = uri.indexOf('?');
        if (queryStart < 0) {
            return uri;
        }
        String kept = Arrays.stream(uri.substring(queryStart + 1).split("&"))
                            .filter(p -> !p.startsWith("x-id="))
                            .collect(Collectors.joining("&"));
        return uri.substring(0, queryStart) + (kept.isEmpty() ? "" : "?" + kept);
    }
}

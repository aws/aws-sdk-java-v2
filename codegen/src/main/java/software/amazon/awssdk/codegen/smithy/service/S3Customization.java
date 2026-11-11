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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.ModifyModelShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.ShapeModifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ParameterHttpMapping;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.CustomOperationContextParam;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * S3 edits that a smithy-build transform cannot make, because Smithy rejects the result or the value exists only in
 * the intermediate model:
 * <ul>
 *     <li>Injects {@code ExpiresString}, a second member on the {@code Expires} header, and {@code SdkPartType}, an
 *     unbound member, as {@code customization.config} defines them.</li>
 *     <li>Keeps {@code PartNumberMarker} and {@code NextPartNumberMarker} integers; Smithy requires pagination tokens
 *     to be strings.</li>
 *     <li>Has HeadBucket and HeadObject throw {@code NoSuchBucket} and {@code NoSuchKey} instead of
 *     {@code NotFound}.</li>
 *     <li>Signs with the S3 variant of SigV4.</li>
 *     <li>Adds the endpoint parameters and operation context parameters from {@code customization.config}.</li>
 *     <li>Keeps members that {@code customization.config} renames, such as {@code CopyObjectRequest.Bucket}, optional
 *     and on their old URI label.</li>
 *     <li>Removes the bucket label and {@code x-id} from request URIs; the endpoint rules place the bucket.</li>
 *     <li>Sends no XML body for a request whose only body members are injected, such as
 *     {@code CopyObjectRequest}.</li>
 * </ul>
 */
@SdkInternalApi
final class S3Customization implements SmithyServiceCustomization {
    private static final String NAMESPACE = "com.amazonaws.s3";
    private static final List<ShapeId> INJECTED = Arrays.asList(s3Member("GetObjectOutput", "ExpiresString"),
                                                                s3Member("HeadObjectOutput", "ExpiresString"),
                                                                s3Member("UploadPartRequest", "SdkPartType"));
    private static final Set<String> APPLIED_CONFIG = appliedConfig();
    private static final Pattern RENAMED_MEMBER_REQUIRED =
        Pattern.compile("shapeModifiers\\.[^.]+\\.modify\\.[^.]+\\.required");
    private static final List<ShapeId> INTEGER_MARKERS = Arrays.asList(s3("PartNumberMarker"),
                                                                       s3("NextPartNumberMarker"));
    private static final ShapeId NOT_FOUND = s3("NotFound");

    @Override
    public Set<String> sdkIds() {
        return Collections.singleton("S3");
    }

    @Override
    public boolean appliesConfig(String key) {
        return APPLIED_CONFIG.contains(key) || RENAMED_MEMBER_REQUIRED.matcher(key).matches();
    }

    private static Set<String> appliedConfig() {
        Set<String> keys = new HashSet<>(Arrays.asList("endpointParameters", "customOperationContextParams"));
        INJECTED.forEach(id -> keys.add("shapeModifiers." + id.getName() + ".inject." + id.getMember().get()));
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        List<Shape> replaced = new ArrayList<>(injectMembers(model, config));
        INTEGER_MARKERS.forEach(id -> replaced.add(integerShape(model.expectShape(id))));
        replaceNotFound(model, "HeadBucket", "NoSuchBucket").ifPresent(replaced::add);
        replaceNotFound(model, "HeadObject", "NoSuchKey").ifPresent(replaced::add);
        return ModelTransformer.create().replaceShapes(model, replaced);
    }

    private static Collection<StructureShape> injectMembers(Model model, CustomizationConfig config) {
        Map<ShapeId, StructureShape.Builder> containers = new LinkedHashMap<>();
        for (ShapeId memberId : INJECTED) {
            Member member = injectedMember(config, memberId);
            if (member != null) {
                containers.computeIfAbsent(memberId.withoutMember(),
                                           id -> model.expectShape(id, StructureShape.class).toBuilder())
                          .addMember(memberShape(memberId, member));
            }
        }
        return containers.values().stream().map(StructureShape.Builder::build).collect(Collectors.toList());
    }

    private static Member injectedMember(CustomizationConfig config, ShapeId memberId) {
        Map<String, ShapeModifier> modifiers = config.getShapeModifiers();
        ShapeModifier modifier = modifiers == null ? null : modifiers.get(memberId.getName());
        if (modifier == null || modifier.getInject() == null) {
            return null;
        }
        String name = memberId.getMember().get();
        return modifier.getInject().stream().map(m -> m.get(name)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static MemberShape memberShape(ShapeId memberId, Member member) {
        MemberShape.Builder shape = MemberShape.builder().id(memberId).target(s3(member.getShape()));
        if (member.getDocumentation() != null) {
            shape.addTrait(new DocumentationTrait(member.getDocumentation()));
        }
        if ("header".equals(member.getLocation())) {
            shape.addTrait(new HttpHeaderTrait(member.getLocationName()));
        } else if (member.getLocation() != null) {
            throw new IllegalStateException(memberId + ": location '" + member.getLocation() + "' is not supported");
        }
        return shape.build();
    }

    private static IntegerShape integerShape(Shape shape) {
        return IntegerShape.builder().id(shape.getId()).addTraits(shape.getAllTraits().values()).build();
    }

    /**
     * Does nothing when the operation does not list {@code NotFound}.
     */
    private static Optional<OperationShape> replaceNotFound(Model model, String operation, String error) {
        return model.getShape(s3(operation))
                    .flatMap(Shape::asOperationShape)
                    .filter(op -> op.getErrors().contains(NOT_FOUND))
                    .map(op -> op.toBuilder()
                                 .errors(op.getErrors().stream()
                                           .map(e -> e.equals(NOT_FOUND) ? s3(error) : e)
                                           .collect(Collectors.toList()))
                                 .build());
    }

    @Override
    public void postprocess(IntermediateModel model) {
        model.getMetadata().setAuthType(AuthType.S3);
        addEndpointParameters(model);
        addOperationContextParams(model);
        // Before the URI edit, which looks for the {Bucket} label this puts back.
        restoreRenamedMembers(model);
        for (ShapeModel shape : model.getShapes().values()) {
            ShapeMarshaller marshaller = shape.getMarshaller();
            if (marshaller != null && marshaller.getRequestUri() != null) {
                marshaller.setRequestUri(withoutBucketAndXId(marshaller.getRequestUri()));
            }
        }
        removeXmlRootOfInjectedBodies(model);
    }

    private static void addEndpointParameters(IntermediateModel model) {
        Map<String, ParameterModel> parameters = model.getCustomizationConfig().getEndpointParameters();
        if (parameters == null) {
            return;
        }
        Map<String, ParameterModel> merged = new LinkedHashMap<>(model.getEndpointParameters());
        parameters.forEach((name, parameter) -> {
            if (merged.putIfAbsent(name, parameter) != null) {
                throw new IllegalStateException("Duplicate endpoint parameter " + name);
            }
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
     * A member that {@code customization.config} renames stays optional, with its old name in the shape's required
     * list, and a URI label keeps the old name.
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
                        restoreRenamedMember(Utils.findShapeModelByC2jName(model, shapeName), oldName,
                                             value.getEmitPropertyName());
                    }
                });
            }
        });
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

    private static String withoutBucketAndXId(String requestUri) {
        String uri = StringUtils.replaceOnce(requestUri, "/{Bucket}", "");
        int queryStart = uri.indexOf('?');
        if (queryStart < 0) {
            return uri;
        }
        String kept = Arrays.stream(uri.substring(queryStart + 1).split("&"))
                            .filter(p -> !p.startsWith("x-id="))
                            .collect(Collectors.joining("&"));
        return uri.substring(0, queryStart) + (kept.isEmpty() ? "" : "?" + kept);
    }

    /**
     * A request whose unbound members are all injected loses its XML root, so it is sent without a body.
     */
    private static void removeXmlRootOfInjectedBodies(IntermediateModel model) {
        Map<String, ShapeModifier> modifiers = model.getCustomizationConfig().getShapeModifiers();
        if (modifiers == null) {
            return;
        }
        modifiers.forEach((shapeName, modifier) -> {
            if (modifier.getInject() == null) {
                return;
            }
            ShapeModel shape = Utils.findShapeModelByC2jNameIfExists(model, shapeName);
            ShapeMarshaller marshaller = shape == null ? null : shape.getMarshaller();
            // Checked before getUnboundMembers, which throws for a shape with a payload member, such as
            // UploadPartRequest.
            if (marshaller == null || marshaller.getLocationName() == null) {
                return;
            }
            Set<String> injected = modifier.getInject().stream()
                                           .flatMap(m -> m.keySet().stream())
                                           .collect(Collectors.toSet());
            if (shape.getUnboundMembers().stream().allMatch(m -> injected.contains(m.getC2jName()))) {
                marshaller.setLocationName(null);
                marshaller.setXmlNameSpaceUri(null);
            }
        });
    }

    private static ShapeId s3(String name) {
        return ShapeId.fromParts(NAMESPACE, name);
    }

    private static ShapeId s3Member(String shape, String member) {
        return ShapeId.fromParts(NAMESPACE, shape, member);
    }
}

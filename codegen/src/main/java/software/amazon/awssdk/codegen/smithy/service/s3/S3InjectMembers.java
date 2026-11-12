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

package software.amazon.awssdk.codegen.smithy.service.s3;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Adds {@code ExpiresString}, a second member on the {@code Expires} header, to the GetObject and HeadObject outputs,
 * and {@code SdkPartType}, an unbound member, to {@code UploadPartRequest}. A member whose target is not in the model
 * is not added: {@code SdkPartType} targets the shape that {@code customSdkShapes} adds.
 */
@SdkInternalApi
final class S3InjectMembers extends AbstractS3Customization {
    private static final String EXPIRES_DOC = "<p>The date and time at which the object is no longer cacheable</p>";
    private static final String PART_TYPE_DOC = "Indicates whether the request is a last part or not. "
                                                + "This field will not be included in the request payload.";
    private static final List<MemberShape> MEMBERS = Arrays.asList(
        expiresString("GetObjectOutput"),
        expiresString("HeadObjectOutput"),
        MemberShape.builder()
                   .id(s3Member("UploadPartRequest", "SdkPartType"))
                   .target(s3("SdkPartType"))
                   .addTrait(new DocumentationTrait(PART_TYPE_DOC))
                   .build());

    @Override
    public boolean appliesConfig(String key) {
        return MEMBERS.stream().anyMatch(m -> key.equals("shapeModifiers." + m.getContainer().getName() + ".inject."
                                                         + m.getMemberName()));
    }

    @Override
    public Model preprocess(Model model, CustomizationConfig config) {
        Map<ShapeId, StructureShape.Builder> containers = new LinkedHashMap<>();
        for (MemberShape member : MEMBERS) {
            if (model.getShape(member.getTarget()).isPresent()) {
                containers.computeIfAbsent(member.getContainer(),
                                           id -> model.expectShape(id, StructureShape.class).toBuilder())
                          .addMember(member);
            }
        }
        List<Shape> replaced = containers.values().stream().map(StructureShape.Builder::build)
                                         .collect(Collectors.toList());
        return ModelTransformer.create().replaceShapes(model, replaced);
    }

    private static MemberShape expiresString(String shape) {
        return MemberShape.builder()
                          .id(s3Member(shape, "ExpiresString"))
                          .target(s3("Expiration"))
                          .addTrait(new DocumentationTrait(EXPIRES_DOC))
                          .addTrait(new HttpHeaderTrait("Expires"))
                          .build();
    }
}

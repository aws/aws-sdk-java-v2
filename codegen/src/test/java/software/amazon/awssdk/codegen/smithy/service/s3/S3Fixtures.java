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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ParameterHttpMapping;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Location;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * A small s3 model, and intermediate-model shapes as the translator builds them.
 */
final class S3Fixtures {
    static final String NAMESPACE = "com.amazonaws.s3#";
    static final String XMLNS = "http://s3.amazonaws.com/doc/2006-03-01/";

    static final String S3_MODEL =
        "$version: \"2.0\"\nnamespace com.amazonaws.s3\n\n"
        + "use aws.api#service\nuse aws.auth#sigv4\nuse aws.protocols#restXml\n\n"
        + "@service(sdkId: \"S3\", arnNamespace: \"s3\")\n"
        + "@sigv4(name: \"s3\")\n"
        + "@restXml(noErrorWrapping: true)\n"
        + "@xmlNamespace(uri: \"http://s3.amazonaws.com/doc/2006-03-01/\")\n"
        + "@smithy.rules#clientContextParams(Region: { type: \"string\", documentation: \"The region\" })\n"
        + "@smithy.rules#endpointRuleSet({\n"
        + "  version: \"1.0\"\n"
        + "  parameters: { Region: { required: false, type: \"string\" } }\n"
        + "  rules: [{ conditions: [], endpoint: { url: \"https://s3.amazonaws.com\" }, type: \"endpoint\" }]\n"
        + "})\n"
        + "service AmazonS3 { version: \"2006-03-01\", operations: [DeleteObjects, HeadBucket, HeadObject] }\n\n"
        + "@http(method: \"POST\", uri: \"/{Bucket}?delete\")\n"
        + "operation DeleteObjects { input: DeleteObjectsRequest }\n\n"
        + "@http(method: \"HEAD\", uri: \"/{Bucket}\")\n"
        + "operation HeadBucket { input: HeadBucketRequest, output: HeadBucketOutput, errors: [NotFound] }\n\n"
        + "@http(method: \"HEAD\", uri: \"/{Bucket}/{Key+}\")\n"
        + "operation HeadObject { input: HeadObjectRequest, output: HeadObjectOutput, errors: [NotFound] }\n\n"
        + "structure DeleteObjectsRequest { @required @httpLabel Bucket: String }\n"
        + "structure HeadBucketRequest { @required @httpLabel Bucket: String }\n"
        + "structure HeadBucketOutput {}\n"
        + "structure HeadObjectRequest { @required @httpLabel Bucket: String, @required @httpLabel Key: String }\n"
        + "structure HeadObjectOutput {}\n"
        + "structure GetObjectOutput {}\n"
        + "structure UploadPartRequest {}\n"
        + "@error(\"client\") structure NotFound {}\n"
        + "@error(\"client\") @httpError(404) structure NoSuchBucket {}\n"
        + "@error(\"client\") @httpError(404) structure NoSuchKey {}\n"
        + "string Expiration\n"
        + "string PartNumberMarker\n"
        + "string NextPartNumberMarker\n";

    /**
     * The shape {@code customSdkShapes} adds for {@code UploadPartRequest.SdkPartType}.
     */
    static final String SDK_PART_TYPE = "string SdkPartType\n";

    private S3Fixtures() {
    }

    static Model s3Model() {
        return s3Model(S3_MODEL + SDK_PART_TYPE);
    }

    static Model s3Model(String smithy) {
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("s3.smithy", smithy)
                    .assemble()
                    .unwrap();
    }

    /**
     * Builds the intermediate model of {@link #s3Model()}, with every s3 customization.
     */
    static IntermediateModel build() {
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(s3Model())
                                                              .customizationConfig(CustomizationConfig.create())
                                                              .build()).build();
    }

    static StructureShape structure(Model model, String name) {
        return model.expectShape(ShapeId.from(NAMESPACE + name), StructureShape.class);
    }

    static IntermediateModel intermediateModel(ShapeModel... shapes) {
        Map<String, ShapeModel> byName = new HashMap<>();
        for (ShapeModel shape : shapes) {
            byName.put(shape.getC2jName(), shape);
        }
        return new IntermediateModel(new Metadata(), new HashMap<>(), byName, CustomizationConfig.create());
    }

    static ShapeModel shape(String name, String requestUri) {
        ShapeModel shape = new ShapeModel(name);
        shape.setMarshaller(new ShapeMarshaller().withRequestUri(requestUri));
        return shape;
    }

    /**
     * A request shape with an XML root, as the translator builds a rest-xml request with a body.
     */
    static ShapeModel xmlRootShape(String name) {
        ShapeModel shape = shape(name, "/{Key+}");
        shape.getMarshaller().setLocationName(name);
        shape.getMarshaller().setXmlNameSpaceUri(XMLNS);
        return shape;
    }

    /**
     * A required member; {@code location} null means unbound.
     */
    static MemberModel member(String name, Location location, String locationName, boolean greedy) {
        MemberModel member = new MemberModel().withC2jName(name).withName(name);
        member.setRequired(true);
        member.setHttp(new ParameterHttpMapping().withLocation(location)
                                                 .withMarshallLocationName(locationName)
                                                 .withUnmarshallLocationName(locationName)
                                                 .setIsGreedy(greedy));
        return member;
    }

    static void assertLocationName(MemberModel member, String locationName) {
        assertThat(member.getHttp().getMarshallLocationName()).isEqualTo(locationName);
        assertThat(member.getHttp().getUnmarshallLocationName()).isEqualTo(locationName);
    }
}

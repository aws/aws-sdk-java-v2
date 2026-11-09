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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ParameterHttpMapping;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ContextParam;
import software.amazon.awssdk.codegen.model.service.Location;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpHeaderTrait;

class S3CustomizationTest {
    private static final String NAMESPACE = "com.amazonaws.s3#";

    private static final String S3_MODEL =
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
        + "service AmazonS3 { version: \"2006-03-01\", operations: [HeadBucket, HeadObject] }\n\n"
        + "@http(method: \"HEAD\", uri: \"/{Bucket}\")\n"
        + "operation HeadBucket { input: HeadBucketRequest, output: HeadBucketOutput, errors: [NotFound] }\n\n"
        + "@http(method: \"HEAD\", uri: \"/{Bucket}/{Key+}\")\n"
        + "operation HeadObject { input: HeadObjectRequest, output: HeadObjectOutput, errors: [NotFound] }\n\n"
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

    private static final String RENAMES =
        "{\"shapeModifiers\": {\"CopyObjectRequest\": {\"modify\": [{"
        + "\"Bucket\": {\"emitPropertyName\": \"DestinationBucket\", \"existingNameDeprecated\": true},"
        + "\"Key\": {\"emitPropertyName\": \"DestinationKey\", \"existingNameDeprecated\": true}}]}}}";

    @Test
    void appliesTheRequiredDropOfRenamedMembers() {
        S3Customization s3 = new S3Customization();
        assertThat(s3.appliesConfig("shapeModifiers.CopyObjectRequest.modify.Bucket.required")).isTrue();
        assertThat(s3.appliesConfig("shapeModifiers.CopyObjectRequest.inject.Bucket")).isFalse();
    }

    @Test
    void renamedMembers_areNotRequiredAndTheRequiredListKeepsTheOldNames() throws IOException {
        IntermediateModel model = model();
        new S3Customization().postprocess(model);
        ShapeModel copy = model.getShapes().get("CopyObjectRequest");
        assertThat(copy.getMemberByC2jName("DestinationBucket").isRequired()).isFalse();
        assertThat(copy.getMemberByC2jName("DestinationKey").isRequired()).isFalse();
        assertThat(copy.getMemberByC2jName("CopySource").isRequired()).isTrue();
        assertThat(copy.getRequired()).containsExactlyInAnyOrder("Bucket", "CopySource", "Key");
    }

    @Test
    void renamedUriLabels_keepTheOldLocationNamesAndUriLabels() throws IOException {
        IntermediateModel model = model();
        new S3Customization().postprocess(model);
        ShapeModel copy = model.getShapes().get("CopyObjectRequest");
        assertLocationName(copy.getMemberByC2jName("DestinationBucket"), "Bucket");
        assertLocationName(copy.getMemberByC2jName("DestinationKey"), "Key");
        assertLocationName(copy.getMemberByC2jName("CopySource"), "x-amz-copy-source");
        assertThat(copy.getMarshaller().getRequestUri()).isEqualTo("/{Key+}");
    }

    @Test
    void membersThatAreNotRenamed_areUnchanged() throws IOException {
        IntermediateModel model = model();
        new S3Customization().postprocess(model);
        ShapeModel get = model.getShapes().get("GetObjectRequest");
        assertThat(get.getMemberByC2jName("Bucket").isRequired()).isTrue();
        assertThat(get.getMemberByC2jName("Key").isRequired()).isTrue();
        assertThat(get.getRequired()).containsExactly("Bucket", "Key");
        assertLocationName(get.getMemberByC2jName("Bucket"), "Bucket");
        assertLocationName(get.getMemberByC2jName("Key"), "Key");
        assertThat(get.getMarshaller().getRequestUri()).isEqualTo("/{Key+}");
    }

    @Test
    void preprocess_withoutShapeModifiers_injectsNoMembers() {
        Model model = new S3Customization().preprocess(s3Model(), CustomizationConfig.create());

        assertThat(structure(model, "GetObjectOutput").getMember("ExpiresString")).isEmpty();
        assertThat(structure(model, "HeadObjectOutput").getMember("ExpiresString")).isEmpty();
        assertThat(structure(model, "UploadPartRequest").getMember("SdkPartType")).isEmpty();
        assertThat(model.expectShape(ShapeId.from(NAMESPACE + "PartNumberMarker")).isIntegerShape()).isTrue();
    }

    @Test
    void preprocess_injectsOnlyTheMembersTheConfigInjects() throws IOException {
        CustomizationConfig config = Jackson.load(CustomizationConfig.class,
            "{\"shapeModifiers\": {\"HeadObjectOutput\": {\"inject\": [{\"ExpiresString\": "
            + "{\"shape\": \"Expiration\", \"location\": \"header\", \"locationName\": \"Expires\"}}]}}}");

        Model model = new S3Customization().preprocess(s3Model(), config);

        MemberShape expires = structure(model, "HeadObjectOutput").getMember("ExpiresString").get();
        assertThat(expires.getTarget()).isEqualTo(ShapeId.from(NAMESPACE + "Expiration"));
        assertThat(expires.expectTrait(HttpHeaderTrait.class).getValue()).isEqualTo("Expires");
        assertThat(structure(model, "GetObjectOutput").getMember("ExpiresString")).isEmpty();
    }

    @Test
    void configEndpointParameters_areKeptAfterUnusedShapesAreRemoved() throws IOException {
        IntermediateModel model = build(Jackson.load(CustomizationConfig.class,
            "{\"endpointParameters\": {\"DeleteObjectKeys\": {\"required\": false, \"type\": \"StringArray\"}}}"));

        assertThat(model.getEndpointParameters()).containsOnlyKeys("Region", "DeleteObjectKeys");
    }

    private static IntermediateModel build(CustomizationConfig config) {
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(s3Model())
                                                              .customizationConfig(config)
                                                              .build()).build();
    }

    private static Model s3Model() {
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("s3.smithy", S3_MODEL)
                    .assemble()
                    .unwrap();
    }

    private static StructureShape structure(Model model, String name) {
        return model.expectShape(ShapeId.from(NAMESPACE + name), StructureShape.class);
    }

    private static void assertLocationName(MemberModel member, String locationName) {
        assertThat(member.getHttp().getMarshallLocationName()).isEqualTo(locationName);
        assertThat(member.getHttp().getUnmarshallLocationName()).isEqualTo(locationName);
    }

    /**
     * The two shapes as the translator builds them: the renamed members required and labelled by their new names.
     */
    private static IntermediateModel model() throws IOException {
        ShapeModel copy = shape("CopyObjectRequest", "/{DestinationBucket}/{DestinationKey+}?x-id=CopyObject");
        copy.addMember(member("CopySource", Location.HEADER, "x-amz-copy-source", false));
        copy.addMember(member("DestinationBucket", Location.URI, "DestinationBucket", false));
        copy.addMember(member("DestinationKey", Location.URI, "DestinationKey", true));
        copy.setRequired(new ArrayList<>(Arrays.asList("CopySource", "DestinationBucket", "DestinationKey")));
        ShapeModel get = shape("GetObjectRequest", "/{Bucket}/{Key+}?x-id=GetObject");
        get.addMember(member("Bucket", Location.URI, "Bucket", false));
        get.addMember(member("Key", Location.URI, "Key", true));
        get.setRequired(new ArrayList<>(Arrays.asList("Bucket", "Key")));
        Map<String, ShapeModel> shapes = new HashMap<>();
        shapes.put(copy.getC2jName(), copy);
        shapes.put(get.getC2jName(), get);
        return new IntermediateModel(new Metadata(), new HashMap<>(), shapes,
                                     Jackson.load(CustomizationConfig.class, RENAMES));
    }

    private static ShapeModel shape(String name, String requestUri) {
        ShapeModel shape = new ShapeModel(name);
        shape.setMarshaller(new ShapeMarshaller().withRequestUri(requestUri));
        return shape;
    }

    private static MemberModel member(String name, Location location, String locationName, boolean greedy) {
        MemberModel member = new MemberModel().withC2jName(name).withName(name);
        member.setRequired(true);
        member.setHttp(new ParameterHttpMapping().withLocation(location)
                                                 .withMarshallLocationName(locationName)
                                                 .withUnmarshallLocationName(locationName)
                                                 .setIsGreedy(greedy));
        if (name.endsWith("Bucket")) {
            ContextParam bucket = new ContextParam();
            bucket.setName("Bucket");
            member.setContextParam(bucket);
        }
        return member;
    }
}

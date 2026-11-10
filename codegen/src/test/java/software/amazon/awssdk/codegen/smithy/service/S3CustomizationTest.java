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

class S3CustomizationTest {
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

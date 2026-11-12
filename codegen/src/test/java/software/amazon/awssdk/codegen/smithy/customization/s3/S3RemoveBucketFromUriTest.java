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

package software.amazon.awssdk.codegen.smithy.customization.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.assertLocationName;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.intermediateModel;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.member;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.shape;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Location;

class S3RemoveBucketFromUriTest {
    @Test
    void renamedMembers_areNotRequiredAndTheRequiredListKeepsTheOldNames() {
        IntermediateModel model = renamedModel();
        new S3RemoveBucketFromUri().postprocess(model);
        ShapeModel copy = model.getShapes().get("CopyObjectRequest");
        assertThat(copy.getMemberByC2jName("DestinationBucket").isRequired()).isFalse();
        assertThat(copy.getMemberByC2jName("DestinationKey").isRequired()).isFalse();
        assertThat(copy.getMemberByC2jName("CopySource").isRequired()).isTrue();
        assertThat(copy.getRequired()).containsExactlyInAnyOrder("Bucket", "CopySource", "Key");
    }

    @Test
    void renamedUriLabels_keepTheOldLocationNamesAndUriLabels() {
        IntermediateModel model = renamedModel();
        new S3RemoveBucketFromUri().postprocess(model);
        ShapeModel copy = model.getShapes().get("CopyObjectRequest");
        assertLocationName(copy.getMemberByC2jName("DestinationBucket"), "Bucket");
        assertLocationName(copy.getMemberByC2jName("DestinationKey"), "Key");
        assertLocationName(copy.getMemberByC2jName("CopySource"), "x-amz-copy-source");
        assertThat(copy.getMarshaller().getRequestUri()).isEqualTo("/{Key+}");
    }

    @Test
    void membersOfOtherRequests_areUnchanged() {
        IntermediateModel model = renamedModel();
        new S3RemoveBucketFromUri().postprocess(model);
        ShapeModel get = model.getShapes().get("GetObjectRequest");
        assertThat(get.getMemberByC2jName("Bucket").isRequired()).isTrue();
        assertThat(get.getMemberByC2jName("Key").isRequired()).isTrue();
        assertThat(get.getRequired()).containsExactly("Bucket", "Key");
        assertLocationName(get.getMemberByC2jName("Bucket"), "Bucket");
        assertLocationName(get.getMemberByC2jName("Key"), "Key");
        assertThat(get.getMarshaller().getRequestUri()).isEqualTo("/{Key+}");
    }

    @Test
    void copyRequestThatIsNotRenamed_keepsItsMembers() {
        ShapeModel copy = shape("CopyObjectRequest", "/{Bucket}/{Key+}?x-id=CopyObject");
        copy.addMember(member("Bucket", Location.URI, "Bucket", false));
        copy.addMember(member("Key", Location.URI, "Key", true));
        copy.setRequired(new ArrayList<>(Arrays.asList("Bucket", "Key")));

        new S3RemoveBucketFromUri().postprocess(intermediateModel(copy));

        assertThat(copy.getMemberByC2jName("Bucket").isRequired()).isTrue();
        assertThat(copy.getRequired()).containsExactly("Bucket", "Key");
        assertThat(copy.getMarshaller().getRequestUri()).isEqualTo("/{Key+}");
    }

    @Test
    void copyRequestWithoutTheBucketMember_fails() {
        ShapeModel copy = shape("CopyObjectRequest", "/{Key+}");
        copy.addMember(member("Key", Location.URI, "Key", true));

        assertThatThrownBy(() -> new S3RemoveBucketFromUri().postprocess(intermediateModel(copy)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("CopyObjectRequest has neither Bucket nor DestinationBucket");
    }

    @Test
    void requestUris_loseTheBucketLabelAndXIdButKeepOtherQueryParameters() {
        ShapeModel create = shape("CreateMultipartUploadRequest", "/{Bucket}/{Key+}?uploads&x-id=CreateMultipartUpload");
        ShapeModel head = shape("HeadBucketRequest", "/{Bucket}");

        new S3RemoveBucketFromUri().postprocess(intermediateModel(create, head));

        assertThat(create.getMarshaller().getRequestUri()).isEqualTo("/{Key+}?uploads");
        assertThat(head.getMarshaller().getRequestUri()).isEmpty();
    }

    /**
     * The shapes as the translator builds them after the renames: the renamed members required and labelled by their
     * new names.
     */
    private static IntermediateModel renamedModel() {
        ShapeModel copy = shape("CopyObjectRequest", "/{DestinationBucket}/{DestinationKey+}?x-id=CopyObject");
        copy.addMember(member("CopySource", Location.HEADER, "x-amz-copy-source", false));
        copy.addMember(member("DestinationBucket", Location.URI, "DestinationBucket", false));
        copy.addMember(member("DestinationKey", Location.URI, "DestinationKey", true));
        copy.setRequired(new ArrayList<>(Arrays.asList("CopySource", "DestinationBucket", "DestinationKey")));
        ShapeModel get = shape("GetObjectRequest", "/{Bucket}/{Key+}?x-id=GetObject");
        get.addMember(member("Bucket", Location.URI, "Bucket", false));
        get.addMember(member("Key", Location.URI, "Key", true));
        get.setRequired(new ArrayList<>(Arrays.asList("Bucket", "Key")));
        return intermediateModel(copy, get);
    }
}

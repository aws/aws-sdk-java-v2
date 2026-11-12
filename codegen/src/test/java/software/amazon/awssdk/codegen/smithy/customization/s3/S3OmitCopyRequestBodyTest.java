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
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.XMLNS;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.intermediateModel;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.member;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.xmlRootShape;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Location;

class S3OmitCopyRequestBodyTest {
    @Test
    void copyRequestWhoseBodyMembersAreAllSourceMembers_hasNoXmlRoot() {
        ShapeModel copy = xmlRootShape("CopyObjectRequest");
        copy.addMember(member("CopySource", Location.HEADER, "x-amz-copy-source", false));
        copy.addMember(member("SourceBucket", null, "SourceBucket", false));
        copy.addMember(member("SourceKey", null, "SourceKey", false));

        new S3OmitCopyRequestBody().postprocess(intermediateModel(copy));

        ShapeMarshaller marshaller = copy.getMarshaller();
        assertThat(marshaller.getLocationName()).isNull();
        assertThat(marshaller.getXmlNameSpaceUri()).isNull();
    }

    @Test
    void copyRequestWithAModeledBodyMember_keepsItsXmlRoot() {
        ShapeModel copy = xmlRootShape("UploadPartCopyRequest");
        copy.addMember(member("Expression", null, "Expression", false));
        copy.addMember(member("SourceBucket", null, "SourceBucket", false));

        new S3OmitCopyRequestBody().postprocess(intermediateModel(copy));

        ShapeMarshaller marshaller = copy.getMarshaller();
        assertThat(marshaller.getLocationName()).isEqualTo("UploadPartCopyRequest");
        assertThat(marshaller.getXmlNameSpaceUri()).isEqualTo(XMLNS);
    }
}

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

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.smithy.service.SmithyServiceCustomizations;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

class S3CustomizationsTest {
    private static final ServiceShape S3 =
        S3Fixtures.s3Model().expectShape(ShapeId.from(S3Fixtures.NAMESPACE + "AmazonS3"), ServiceShape.class);

    @Test
    void appliesConfig_claimsTheSettingsTheS3CustomizationsApply() {
        assertThat(Arrays.asList("endpointParameters",
                                 "customOperationContextParams",
                                 "shapeModifiers.GetObjectOutput.inject.ExpiresString",
                                 "shapeModifiers.HeadObjectOutput.inject.ExpiresString",
                                 "shapeModifiers.UploadPartRequest.inject.SdkPartType",
                                 "shapeModifiers.CopyObjectRequest.modify.Bucket.required",
                                 "shapeModifiers.CopyObjectRequest.modify.Key.required",
                                 "shapeModifiers.UploadPartCopyRequest.modify.Bucket.required",
                                 "shapeModifiers.UploadPartCopyRequest.modify.Key.required"))
            .allMatch(key -> SmithyServiceCustomizations.appliesConfig(S3, key));
    }

    @Test
    void appliesConfig_leavesOtherSettingsToTheGenerator() {
        assertThat(Arrays.asList("shapeModifiers.CopyObjectRequest.inject.SourceBucket",
                                 "shapeModifiers.CopyObjectRequest.modify.Bucket.emitPropertyName",
                                 "shapeModifiers.PutObjectRequest.modify.Bucket.required",
                                 "customSdkShapes",
                                 "renameShapes"))
            .noneMatch(key -> SmithyServiceCustomizations.appliesConfig(S3, key));
    }
}

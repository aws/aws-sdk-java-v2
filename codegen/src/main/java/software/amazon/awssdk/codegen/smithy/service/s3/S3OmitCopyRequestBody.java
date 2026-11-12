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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

/**
 * Sends CopyObject and UploadPartCopy without an XML body when their only unbound members are {@code SourceBucket},
 * {@code SourceKey} and {@code SourceVersionId}.
 */
@SdkInternalApi
final class S3OmitCopyRequestBody extends AbstractS3Customization {
    private static final List<String> COPY_REQUESTS = Arrays.asList("CopyObjectRequest", "UploadPartCopyRequest");
    private static final Set<String> SOURCE_MEMBERS =
        new HashSet<>(Arrays.asList("SourceBucket", "SourceKey", "SourceVersionId"));

    @Override
    public void postprocess(IntermediateModel model) {
        for (String shapeName : COPY_REQUESTS) {
            ShapeModel shape = Utils.findShapeModelByC2jNameIfExists(model, shapeName);
            ShapeMarshaller marshaller = shape == null ? null : shape.getMarshaller();
            if (marshaller != null && marshaller.getLocationName() != null
                && shape.getUnboundMembers().stream().allMatch(m -> SOURCE_MEMBERS.contains(m.getC2jName()))) {
                marshaller.setLocationName(null);
                marshaller.setXmlNameSpaceUri(null);
            }
        }
    }
}

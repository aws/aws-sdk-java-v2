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
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.smithy.service.SmithyServiceCustomization;

/**
 * The s3 customizations.
 */
@SdkInternalApi
public final class S3Customizations {
    private S3Customizations() {
    }

    public static List<SmithyServiceCustomization> all() {
        return Arrays.asList(new S3InjectMembers(),
                             new S3RetypePartNumberMarkers(),
                             new S3ReplaceNotFound(),
                             new S3SetAuthType(),
                             new S3AddDeleteObjectKeys(),
                             new S3RemoveBucketFromUri(),
                             new S3OmitCopyRequestBody());
    }
}

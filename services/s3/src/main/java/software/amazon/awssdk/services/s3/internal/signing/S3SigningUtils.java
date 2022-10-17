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

package software.amazon.awssdk.services.s3.internal.signing;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils;
import software.amazon.awssdk.services.s3.internal.resource.S3ArnConverter;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3.model.S3Request;

/**
 * Utilities for working with S3 specific signing
 */
@SdkInternalApi
public final class S3SigningUtils {
    
    private S3SigningUtils() {
    }

    public static Optional<Signer> internalSignerOverride(S3Request originalRequest) {
        return originalRequest.getValueForField("Bucket", String.class)
                              .filter(S3EndpointUtils::isArn)
                              .flatMap(S3SigningUtils::getS3ResourceSigner);
    }

    private static Optional<Signer> getS3ResourceSigner(String name) {
        S3Resource resolvedS3Resource = S3ArnConverter.create().convertArn(Arn.fromString(name));
        return resolvedS3Resource.overrideSigner();
    }
}

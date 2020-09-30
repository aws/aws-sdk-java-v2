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

package software.amazon.awssdk.services.s3control.internal;


import static software.amazon.awssdk.services.s3.internal.resource.S3ArnUtils.parseOutpostArn;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.arns.ArnResource;
import software.amazon.awssdk.services.s3.internal.resource.ArnConverter;
import software.amazon.awssdk.services.s3.internal.resource.IntermediateOutpostResource;
import software.amazon.awssdk.services.s3.internal.resource.OutpostResourceType;
import software.amazon.awssdk.services.s3.internal.resource.S3AccessPointResource;
import software.amazon.awssdk.services.s3.internal.resource.S3OutpostResource;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3control.S3ControlBucketResource;

@SdkInternalApi
public final class S3ControlArnConverter implements ArnConverter<S3Resource> {
    private static final S3ControlArnConverter INSTANCE = new S3ControlArnConverter();

    private S3ControlArnConverter() {
    }

    /**
     * Gets a static singleton instance of an {@link S3ControlArnConverter}.
     *
     * @return A static instance of an {@link S3ControlArnConverter}.
     */
    public static S3ControlArnConverter getInstance() {
        return INSTANCE;
    }

    @Override
    public S3Resource convertArn(Arn arn) {
        S3ControlResourceType s3ResourceType;

        try {
            s3ResourceType =
                arn.resource().resourceType().map(S3ControlResourceType::fromValue)
                   .orElseThrow(() -> new IllegalArgumentException("resource type cannot be null"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ARN type '" + arn.resource().resourceType() + "'");
        }

        switch (s3ResourceType) {
            case OUTPOST:
                return parseS3OutpostArn(arn);
            default:
                throw new IllegalArgumentException("Unknown ARN type '" + arn.resource().resourceType() + "'");
        }

    }

    private S3Resource parseS3OutpostArn(Arn arn) {
        IntermediateOutpostResource intermediateOutpostResource = parseOutpostArn(arn);
        ArnResource outpostSubresource = intermediateOutpostResource.outpostSubresource();
        String subResource = outpostSubresource.resource();
        OutpostResourceType outpostResourceType;
        try {
            outpostResourceType = outpostSubresource.resourceType().map(OutpostResourceType::fromValue)
                                                    .orElseThrow(() -> new IllegalArgumentException("resource type cannot be "
                                                                                                    + "null"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown outpost ARN type '" + outpostSubresource.resourceType() + "'");
        }

        String outpostId = intermediateOutpostResource.outpostId();

        switch (outpostResourceType) {
            case OUTPOST_BUCKET:
                return S3ControlBucketResource.builder()
                                              .bucketName(subResource)
                                              .parentS3Resource(S3OutpostResource.builder()
                                                                              .partition(arn.partition())
                                                                              .region(arn.region().orElse(null))
                                                                              .accountId(arn.accountId().orElse(null))
                                                                              .outpostId(outpostId)
                                                                              .build())
                                              .build();

            case OUTPOST_ACCESS_POINT:
                return S3AccessPointResource.builder()
                                            .accessPointName(subResource)
                                            .parentS3Resource(S3OutpostResource.builder()
                                                                                   .partition(arn.partition())
                                                                                   .region(arn.region().orElse(null))
                                                                                   .accountId(arn.accountId().orElse(null))
                                                                                   .outpostId(outpostId)
                                                                                   .build())
                                            .build();
            default:
                throw new IllegalArgumentException("Unknown outpost ARN type '" + outpostSubresource.resourceType() + "'");
        }
    }

}

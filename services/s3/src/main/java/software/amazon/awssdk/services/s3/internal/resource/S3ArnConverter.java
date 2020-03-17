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

package software.amazon.awssdk.services.s3.internal.resource;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.Arn;

/**
 * An implementation of {@link ArnConverter} that can be used to convert valid {@link Arn} representations of s3
 * resources into {@link S3Resource} objects. To fetch an instance of this class, use the singleton getter method
 * {@link #create()}.
 */
@SdkInternalApi
public final class S3ArnConverter implements ArnConverter<S3Resource> {
    private static final S3ArnConverter INSTANCE = new S3ArnConverter();

    private S3ArnConverter() {
    }

    /**
     * Gets a static singleton instance of an {@link S3ArnConverter}.
     * @return A static instance of an {@link S3ArnConverter}.
     */
    public static S3ArnConverter create() {
        return INSTANCE;
    }

    /**
     * Converts a valid ARN representation of an S3 resource into a {@link S3Resource} object.
     * @param arn The ARN to convert.
     * @return An {@link S3Resource} object as specified by the ARN.
     * @throws IllegalArgumentException if the ARN is not a valid representation of an S3 resource supported by this
     * SDK.
     */
    @Override
    public S3Resource convertArn(Arn arn) {
        Arn v2Arn = convertToV2Arn(arn);
        S3ResourceType s3ResourceType;

        if (!v2Arn.resource().resourceType().isPresent()) {
            throw new IllegalArgumentException("Unknown ARN type");
        }

        try {
            s3ResourceType =
                S3ResourceType.fromValue(v2Arn.resource().resourceType().get());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ARN type '" + v2Arn.resource().resourceType().get() + "'");
        }

        switch (s3ResourceType) {
            case OBJECT:
                return parseS3ObjectArn(v2Arn);
            case ACCESS_POINT:
                return parseS3AccessPointArn(v2Arn);
            case BUCKET:
                return parseS3BucketArn(v2Arn);
            default:
                throw new IllegalArgumentException("Unknown ARN type '" + s3ResourceType + "'");
        }
    }

    private Arn convertToV2Arn(Arn arn) {
        if (!isV1Arn(arn)) {
            return arn;
        }

        String resource = arn.resourceAsString();

        if (resource.contains("/")) {
            return arn.toBuilder().resource("object:" + arn.resourceAsString()).build();
        } else {
            return arn.toBuilder().resource("bucket_name:" + arn.resourceAsString()).build();
        }
    }

    private S3BucketResource parseS3BucketArn(Arn arn) {
        return S3BucketResource.builder()
                               .partition(arn.partition())
                               .region(arn.region().orElse(null))
                               .accountId(arn.accountId().orElse(null))
                               .bucketName(arn.resource().resource())
                               .build();
    }

    private S3AccessPointResource parseS3AccessPointArn(Arn arn) {
        return S3AccessPointResource.builder()
                                    .partition(arn.partition())
                                    .region(arn.region().orElse(null))
                                    .accountId(arn.accountId().orElse(null))
                                    .accessPointName(arn.resource().resource())
                                    .build();
    }

    private S3ObjectResource parseS3ObjectArn(Arn arn) {
        String resourceString = arn.resource().resource();
        String [] splitResourceString = resourceString.split("/");

        if (splitResourceString.length < 2) {
            throw new IllegalArgumentException("Invalid format for S3 object resource ARN");
        }

        String bucketName = splitResourceString[0];
        String key = splitResourceString[1];

        return S3ObjectResource.builder()
                               .partition(arn.partition())
                               .region(arn.region().orElse(null))
                               .accountId(arn.accountId().orElse(null))
                               .bucketName(bucketName)
                               .key(key)
                               .build();
    }

    private boolean isV1Arn(Arn arn) {
        return !arn.accountId().isPresent() && !arn.region().isPresent();
    }
}

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

import static software.amazon.awssdk.services.s3.internal.resource.S3ArnUtils.parseOutpostArn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.arns.ArnResource;

/**
 * An implementation of {@link ArnConverter} that can be used to convert valid {@link Arn} representations of s3
 * resources into {@link S3Resource} objects. To fetch an instance of this class, use the singleton getter method
 * {@link #create()}.
 */
@SdkInternalApi
public final class S3ArnConverter implements ArnConverter<S3Resource> {
    private static final S3ArnConverter INSTANCE = new S3ArnConverter();
    private static final Pattern OBJECT_AP_PATTERN = Pattern.compile("^([0-9a-zA-Z-]+)/object/(.*)$");

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
        if (isV1Arn(arn)) {
            return convertV1Arn(arn);
        }
        S3ResourceType s3ResourceType;

        String resourceType = arn.resource().resourceType().orElseThrow(() -> new IllegalArgumentException("Unknown ARN type"));

        try {
            s3ResourceType =
                S3ResourceType.fromValue(resourceType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ARN type '" + arn.resource().resourceType().get() + "'");
        }

        // OBJECT is a sub-resource under ACCESS_POINT and BUCKET and will not be recognized as a primary ARN resource
        // type
        switch (s3ResourceType) {
            case ACCESS_POINT:
                return parseS3AccessPointArn(arn);
            case BUCKET:
                return parseS3BucketArn(arn);
            case OUTPOST:
                return parseS3OutpostAccessPointArn(arn);
            default:
                throw new IllegalArgumentException("Unknown ARN type '" + s3ResourceType + "'");
        }
    }

    private S3Resource convertV1Arn(Arn arn) {
        String resource = arn.resourceAsString();
        String[] splitResource = resource.split("/", 2);

        if (splitResource.length > 1) {
            // Bucket/key
            S3BucketResource parentBucket = S3BucketResource.builder()
                                                            .partition(arn.partition())
                                                            .bucketName(splitResource[0])
                                                            .build();

            return S3ObjectResource.builder()
                                   .parentS3Resource(parentBucket)
                                   .key(splitResource[1])
                                   .build();
        } else {
            // Just bucket
            return S3BucketResource.builder()
                                   .partition(arn.partition())
                                   .bucketName(resource)
                                   .build();
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

    private S3Resource parseS3AccessPointArn(Arn arn) {
        Matcher objectMatcher = OBJECT_AP_PATTERN.matcher(arn.resource().resource());

        if (objectMatcher.matches()) {
            // ARN is actually an object addressed through an access-point
            String accessPointName = objectMatcher.group(1);
            String objectKey = objectMatcher.group(2);
            S3AccessPointResource parentResource =
                S3AccessPointResource.builder()
                                     .partition(arn.partition())
                                     .region(arn.region().orElse(null))
                                     .accountId(arn.accountId().orElse(null))
                                     .accessPointName(accessPointName)
                                     .build();

            return S3ObjectResource.builder()
                                   .parentS3Resource(parentResource)
                                   .key(objectKey)
                                   .build();
        }

        return S3AccessPointResource.builder()
                                    .partition(arn.partition())
                                    .region(arn.region().orElse(null))
                                    .accountId(arn.accountId().orElse(null))
                                    .accessPointName(arn.resource().resource())
                                    .build();
    }

    private S3Resource parseS3OutpostAccessPointArn(Arn arn) {
        IntermediateOutpostResource intermediateOutpostResource = parseOutpostArn(arn);
        ArnResource outpostSubResource = intermediateOutpostResource.outpostSubresource();

        String resourceType = outpostSubResource.resourceType()
                                                .orElseThrow(() -> new IllegalArgumentException("Unknown ARN type"));

        if (!OutpostResourceType.OUTPOST_ACCESS_POINT.toString().equals(resourceType)) {
            throw new IllegalArgumentException("Unknown outpost ARN type '" + outpostSubResource.resourceType() + "'");
        }

        return S3AccessPointResource.builder()
                                    .accessPointName(outpostSubResource.resource())
                                    .parentS3Resource(S3OutpostResource.builder()
                                                                .partition(arn.partition())
                                                                .region(arn.region().orElse(null))
                                                                .accountId(arn.accountId().orElse(null))
                                                                .outpostId(intermediateOutpostResource.outpostId())
                                                                .build())
                                    .build();
    }


    private boolean isV1Arn(Arn arn) {
        return !arn.accountId().isPresent() && !arn.region().isPresent();
    }
}

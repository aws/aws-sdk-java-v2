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
import software.amazon.awssdk.arns.ArnResource;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public class S3ArnUtils {
    private static final int OUTPOST_ID_START_INDEX = "outpost".length() + 1;

    private S3ArnUtils() {
    }

    public static S3AccessPointResource parseS3AccessPointArn(Arn arn) {
        return S3AccessPointResource.builder()
                                    .partition(arn.partition())
                                    .region(arn.region().orElse(null))
                                    .accountId(arn.accountId().orElse(null))
                                    .accessPointName(arn.resource().resource())
                                    .build();
    }

    public static IntermediateOutpostResource parseOutpostArn(Arn arn) {
        String resource = arn.resourceAsString();

        Integer outpostIdEndIndex = null;

        for (int i = OUTPOST_ID_START_INDEX; i < resource.length(); ++i) {
            char ch = resource.charAt(i);

            if (ch == ':' || ch == '/') {
                outpostIdEndIndex = i;
                break;
            }
        }

        if (outpostIdEndIndex == null) {
            throw new IllegalArgumentException("Invalid format for S3 outpost ARN, missing outpostId");
        }

        String outpostId = resource.substring(OUTPOST_ID_START_INDEX, outpostIdEndIndex);

        if (StringUtils.isEmpty(outpostId)) {
            throw new IllegalArgumentException("Invalid format for S3 outpost ARN, missing outpostId");
        }

        String subresource = resource.substring(outpostIdEndIndex + 1);

        if (StringUtils.isEmpty(subresource)) {
            throw new IllegalArgumentException("Invalid format for S3 outpost ARN");
        }

        return IntermediateOutpostResource.builder()
                                          .outpostId(outpostId)
                                          .outpostSubresource(ArnResource.fromString(subresource))
                                          .build();
    }
}

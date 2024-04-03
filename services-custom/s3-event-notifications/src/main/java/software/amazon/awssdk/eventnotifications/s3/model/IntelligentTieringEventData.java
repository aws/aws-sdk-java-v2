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

package software.amazon.awssdk.eventnotifications.s3.model;


import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;

/**
 * The IntelligentTieringEventData key is only visible for
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/intelligent-tiering.html">S3 Intelligent-Tiering</a>
 * related events.
 */
@SdkPublicApi
public class IntelligentTieringEventData {

    private final String destinationAccessTier;

    public IntelligentTieringEventData(String destinationAccessTier) {
        this.destinationAccessTier = destinationAccessTier;
    }

    public String getDestinationAccessTier() {
        return destinationAccessTier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntelligentTieringEventData that = (IntelligentTieringEventData) o;

        return Objects.equals(destinationAccessTier, that.destinationAccessTier);
    }

    @Override
    public int hashCode() {
        return destinationAccessTier != null ? destinationAccessTier.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("IntelligentTieringEventDataEntity")
                       .add("destinationAccessTier", destinationAccessTier)
                       .build();
    }
}

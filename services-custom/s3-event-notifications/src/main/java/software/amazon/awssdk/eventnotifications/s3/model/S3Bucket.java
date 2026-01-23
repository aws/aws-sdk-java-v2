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
 * Bucket information.
 */
@SdkPublicApi
public class S3Bucket {

    private final String name;
    private final UserIdentity ownerIdentity;
    private final String arn;

    public S3Bucket(String name, UserIdentity ownerIdentity, String arn) {
        this.name = name;
        this.ownerIdentity = ownerIdentity;
        this.arn = arn;
    }

    /**
     * @return the bucket name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the user identity containing the Amazon customer ID of the bucket owner.
     */
    public UserIdentity getOwnerIdentity() {
        return ownerIdentity;
    }

    /**
     * @return The bucket ARN.
     */
    public String getArn() {
        return arn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3Bucket s3Bucket = (S3Bucket) o;

        if (!Objects.equals(name, s3Bucket.name)) {
            return false;
        }
        if (!Objects.equals(ownerIdentity, s3Bucket.ownerIdentity)) {
            return false;
        }
        return Objects.equals(arn, s3Bucket.arn);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (ownerIdentity != null ? ownerIdentity.hashCode() : 0);
        result = 31 * result + (arn != null ? arn.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("S3Bucket")
                       .add("name", name)
                       .add("ownerIdentity", ownerIdentity)
                       .add("arn", arn)
                       .build();
    }
}

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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A representation of an AWS S3 resource. See {@link S3ResourceType} for a list and description of all valid types.
 */
@SdkInternalApi
public interface S3Resource extends AwsResource {
    /**
     * Gets the type of S3 resource represented by this object (e.g.: 'bucket_name'). See {@link S3ResourceType} for
     * a list and description of all valid types.
     * @return the string name of the S3 resource type.
     */
    String type();

    /**
     * Gets the optional parent resource.
     * @return the optional parent resource.
     */
    default Optional<S3Resource> parentS3Resource() {
        return Optional.empty();
    }
}

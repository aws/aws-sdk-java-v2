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
 * An abstract representation of an AWS Resource. Provides an interface to properties that are common across all AWS
 * resource types. Services may provide concrete implementations that can be found in each service module.
 */
@SdkInternalApi
public interface AwsResource {
    /**
     * Gets the partition associated with the AWS Resource (e.g.: 'aws') if one has been specified.
     * @return the optional value for the partition.
     */
    Optional<String> partition();

    /**
     * Gets the region associated with the AWS Resource (e.g.: 'us-east-1') if one has been specified.
     * @return the optional value for the region.
     */
    Optional<String> region();

    /**
     * Gets the account ID associated with the AWS Resource if one has been specified.
     * @return the optional value for the account ID.
     */
    Optional<String> accountId();
}

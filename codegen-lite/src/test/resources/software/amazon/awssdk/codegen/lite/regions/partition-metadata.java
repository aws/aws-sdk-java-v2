/*
 * Copyright 2013-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.regions.partitionmetadata;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.regions.PartitionMetadata;

@Generated("software.amazon.awssdk:codegen")
public final class AwsPartitionMetadata implements PartitionMetadata {
    private static final String DNS_SUFFIX = "amazonaws.com";

    private static final String HOSTNAME = "{service}.{region}.{dnsSuffix}";

    private static final String ID = "aws";

    private static final String PARTITION_NAME = "AWS Standard";

    private static final String REGION_REGEX = "^(us|eu|ap|sa|ca)\\-\\w+\\-\\d+$";

    @Override
    public String dnsSuffix() {
        return DNS_SUFFIX;
    }

    @Override
    public String hostname() {
        return HOSTNAME;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String partitionName() {
        return PARTITION_NAME;
    }

    @Override
    public String regionRegex() {
        return REGION_REGEX;
    }
}

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

package software.amazon.awssdk.core.rules;

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Lazy;

@SdkInternalApi
public final class DefaultPartitionDataProvider implements PartitionDataProvider {
    private static final String DEFAULT_PARTITIONS_DATA = "/software/amazon/awssdk/core/rules/partitions.json";

    private static final Lazy<Partitions> PARTITIONS = new Lazy<>(DefaultPartitionDataProvider::doLoadPartitions);

    @Override
    public Partitions loadPartitions() {
        return PARTITIONS.getValue();
    }

    private static Partitions doLoadPartitions() {
        InputStream json = DefaultPartitionDataProvider.class.getResourceAsStream(DEFAULT_PARTITIONS_DATA);
        try {
            return Partitions.fromNode(JsonNode.parser().parse(json));
        } finally {
            IoUtils.closeQuietly(json, null);
        }
    }
}

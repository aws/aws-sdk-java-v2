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

package software.amazon.awssdk.benchmark.enhanced.dynamodb;

import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

public final class V2TestDynamoDbGetItemClient extends V2TestDynamoDbBaseClient {
    private final GetItemResponse getItemResponse;

    public V2TestDynamoDbGetItemClient(Blackhole bh, GetItemResponse getItemResponse) {
        super(bh);
        this.getItemResponse = getItemResponse;
    }

    @Override
    public GetItemResponse getItem(GetItemRequest getItemRequest) {
        bh.consume(getItemRequest);
        return getItemResponse;
    }
}

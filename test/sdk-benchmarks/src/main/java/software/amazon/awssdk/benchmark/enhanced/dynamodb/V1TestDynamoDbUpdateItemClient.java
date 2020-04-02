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

import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import org.openjdk.jmh.infra.Blackhole;

public class V1TestDynamoDbUpdateItemClient extends V1TestDynamoDbBaseClient {
    private final UpdateItemResult updateItemResult;

    public V1TestDynamoDbUpdateItemClient(Blackhole bh, UpdateItemResult updateItemResult) {
        super(bh);
        this.updateItemResult = updateItemResult;
    }

    @Override
    public UpdateItemResult updateItem(UpdateItemRequest request) {
        bh.consume(request);
        return updateItemResult;
    }
}

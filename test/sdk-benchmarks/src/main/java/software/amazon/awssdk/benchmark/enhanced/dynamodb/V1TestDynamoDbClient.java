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

import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import org.openjdk.jmh.infra.Blackhole;

public final class V1TestDynamoDbClient extends AbstractAmazonDynamoDB {
    private static final UpdateItemResult UPDATE_ITEM_RESULT = new UpdateItemResult();
    private static final PutItemResult PUT_ITEM_RESULT = new PutItemResult();

    private final Blackhole bh;
    private final GetItemResult getItemResult;

    public V1TestDynamoDbClient(Blackhole bh, GetItemResult getItemResult) {
        this.bh = bh;
        this.getItemResult = getItemResult;
    }

    @Override
    public GetItemResult getItem(GetItemRequest request) {
        return getItemResult;
    }

    @Override
    public UpdateItemResult updateItem(UpdateItemRequest request) {
        bh.consume(request);
        return UPDATE_ITEM_RESULT;
    }

    @Override
    public PutItemResult putItem(PutItemRequest request) {
        bh.consume(request);
        return PUT_ITEM_RESULT;
    }
}

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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

/**
 * Defines parameters used to retrieve an item from a DynamoDb table using the getItem() operation (such as
 * {@link DynamoDbTable#getItem(GetItemEnhancedRequest)} or {@link DynamoDbAsyncTable#getItem(GetItemEnhancedRequest)}).
 * <p>
 * A valid request object must contain a primary {@link Key} to reference the item to get.
 */
// Records: https://openjdk.java.net/jeps/359
@SdkPublicApi
public final record GetItemEnhancedRequest(Key key, Boolean consistentRead)  {

}

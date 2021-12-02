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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ItemCollectionMetrics;

public class PutItemEnhancedResponseTest {
    @Test
    public void builder_minimal() {
        PutItemEnhancedResponse<FakeItem> builtObject = PutItemEnhancedResponse.builder(FakeItem.class).build();

        assertThat(builtObject.attributes()).isNull();
        assertThat(builtObject.consumedCapacity()).isNull();
        assertThat(builtObject.itemCollectionMetrics()).isNull();
    }

    @Test
    public void builder_maximal() {
        FakeItem fakeItem = createUniqueFakeItem();
        ConsumedCapacity consumedCapacity = ConsumedCapacity.builder().tableName("MyTable").build();
        Map<String, AttributeValue> collectionKey = new HashMap<>();
        collectionKey.put("foo", AttributeValue.builder().s("bar").build());

        ItemCollectionMetrics itemCollectionMetrics = ItemCollectionMetrics.builder().itemCollectionKey(collectionKey).build();

        PutItemEnhancedResponse<FakeItem> builtObject = PutItemEnhancedResponse.builder(FakeItem.class)
                                                                               .attributes(fakeItem)
                                                                               .consumedCapacity(consumedCapacity)
                                                                               .itemCollectionMetrics(itemCollectionMetrics)
                                                                               .build();


        assertThat(builtObject.attributes()).isEqualTo(fakeItem);
        assertThat(builtObject.consumedCapacity()).isEqualTo(consumedCapacity);
        assertThat(builtObject.itemCollectionMetrics()).isEqualTo(itemCollectionMetrics);
    }

    @Test
    public void equals_self() {
        PutItemEnhancedResponse<FakeItem> builtObject = PutItemEnhancedResponse.builder(FakeItem.class).build();

        assertThat(builtObject).isEqualTo(builtObject);
    }

    @Test
    public void equals_differentType() {
        PutItemEnhancedResponse<FakeItem> builtObject = PutItemEnhancedResponse.builder(FakeItem.class).build();

        assertThat(builtObject).isNotEqualTo(new Object());
    }

    @Test
    public void equals_attributesNotEqual() {
        FakeItem fakeItem1 = createUniqueFakeItem();
        FakeItem fakeItem2 = createUniqueFakeItem();

        PutItemEnhancedResponse<FakeItem> builtObject1 = PutItemEnhancedResponse.builder(FakeItem.class)
                                                                                .attributes(fakeItem1)
                                                                                .build();
        PutItemEnhancedResponse<FakeItem> builtObject2 = PutItemEnhancedResponse.builder(FakeItem.class)
                                                                                .attributes(fakeItem2)
                                                                                .build();
        assertThat(builtObject1).isNotEqualTo(builtObject2);
    }

    @Test
    public void hashCode_minimal() {
        PutItemEnhancedResponse<FakeItem> emptyResponse = PutItemEnhancedResponse.builder(FakeItem.class).build();

        assertThat(emptyResponse.hashCode()).isEqualTo(0);
    }

    @Test
    public void hashCode_includesAttributes() {
        PutItemEnhancedResponse<FakeItem> emptyResponse = PutItemEnhancedResponse.builder(FakeItem.class).build();

        FakeItem fakeItem = createUniqueFakeItem();

        PutItemEnhancedResponse<FakeItem> containsAttributes = PutItemEnhancedResponse.builder(FakeItem.class)
                                                                                      .attributes(fakeItem)
                                                                                      .build();

        assertThat(containsAttributes.hashCode()).isNotEqualTo(emptyResponse.hashCode());
    }

    @Test
    public void hashCode_includesConsumedCapacity() {
        PutItemEnhancedResponse<FakeItem> emptyResponse = PutItemEnhancedResponse.builder(FakeItem.class).build();

        ConsumedCapacity consumedCapacity = ConsumedCapacity.builder().tableName("MyTable").build();


        PutItemEnhancedResponse<FakeItem> containsConsumedCapacity = PutItemEnhancedResponse.builder(FakeItem.class)
                                                                                            .consumedCapacity(consumedCapacity)
                                                                                            .build();

        assertThat(containsConsumedCapacity.hashCode()).isNotEqualTo(emptyResponse.hashCode());
    }

    @Test
    public void hashCode_includesItemCollectionMetrics() {
        PutItemEnhancedResponse<FakeItem> emptyResponse = PutItemEnhancedResponse.builder(FakeItem.class).build();

        Map<String, AttributeValue> collectionKey = new HashMap<>();
        collectionKey.put("foo", AttributeValue.builder().s("bar").build());

        ItemCollectionMetrics itemCollectionMetrics = ItemCollectionMetrics.builder().itemCollectionKey(collectionKey).build();

        PutItemEnhancedResponse<FakeItem> containsItemCollectionMetrics = PutItemEnhancedResponse.builder(FakeItem.class)
                                                                                                 .itemCollectionMetrics(itemCollectionMetrics)
                                                                                                 .build();

        assertThat(containsItemCollectionMetrics.hashCode()).isNotEqualTo(emptyResponse.hashCode());
    }
}

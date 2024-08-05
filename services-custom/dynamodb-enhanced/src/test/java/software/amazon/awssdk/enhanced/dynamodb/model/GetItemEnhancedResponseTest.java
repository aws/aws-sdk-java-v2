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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;

@RunWith(MockitoJUnitRunner.class)
public class GetItemEnhancedResponseTest {

    @Test
    public void builder_minimal() {
        GetItemEnhancedResponse<FakeItem> response = GetItemEnhancedResponse.<FakeItem>builder().build();
        assertThat(response.attributes(), is(nullValue()));
        assertThat(response.consumedCapacity(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        FakeItem item = createUniqueFakeItem();
        ConsumedCapacity consumedCapacity = ConsumedCapacity.builder()
                                                            .capacityUnits(10.0)
                                                            .build();
        GetItemEnhancedResponse<FakeItem> response = GetItemEnhancedResponse.<FakeItem>builder()
                                                                            .attributes(item)
                                                                            .consumedCapacity(consumedCapacity)
                                                                            .build();

        assertThat(response.attributes(), is(item));
        assertThat(response.consumedCapacity(), is(consumedCapacity));
    }

    @Test
    public void equals_self() {
        GetItemEnhancedResponse<FakeItem> builtObject1 = GetItemEnhancedResponse.<FakeItem>builder().build();
        assertThat(builtObject1, equalTo(builtObject1));

        ConsumedCapacity consumedCapacity = ConsumedCapacity.builder()
                                                            .capacityUnits(10.0)
                                                            .build();
        GetItemEnhancedResponse<FakeItem> builtObject2 = GetItemEnhancedResponse.<FakeItem>builder()
                                                                                .attributes(createUniqueFakeItem())
                                                                                .consumedCapacity(consumedCapacity)
                                                                                .build();
        assertThat(builtObject2, equalTo(builtObject2));
    }

    @Test
    public void equals_differentType() {
        GetItemEnhancedResponse<FakeItem> response = GetItemEnhancedResponse.<FakeItem>builder().build();
        assertThat(response, not(equalTo(new Object())));
    }

    @Test
    public void equals_attributesNotEqual() {
        FakeItem fakeItem1 = createUniqueFakeItem();
        FakeItem fakeItem2 = createUniqueFakeItem();

        GetItemEnhancedResponse<FakeItem> builtObject1 = GetItemEnhancedResponse.<FakeItem>builder()
                                                                                .attributes(fakeItem1)
                                                                                .build();
        GetItemEnhancedResponse<FakeItem> builtObject2 = GetItemEnhancedResponse.<FakeItem>builder()
                                                                                .attributes(fakeItem2)
                                                                                .build();

        Assertions.assertThat(builtObject1).isNotEqualTo(builtObject2);
    }

    @Test
    public void hashCode_minimal() {
        GetItemEnhancedResponse<FakeItem> response = GetItemEnhancedResponse.<FakeItem>builder().build();
        assertThat(response.hashCode(), equalTo(0));
    }

    @Test
    public void hashCode_includesAttributes() {
        GetItemEnhancedResponse<FakeItem> builtObject1 = GetItemEnhancedResponse.<FakeItem>builder().build();
        GetItemEnhancedResponse<FakeItem> builtObject2 = GetItemEnhancedResponse.<FakeItem>builder()
                                                                                .attributes(createUniqueFakeItem())
                                                                                .build();

        assertThat(builtObject1.hashCode(), not(equalTo(builtObject2.hashCode())));
    }

    @Test
    public void hashCode_includesConsumedCapacity() {
        GetItemEnhancedResponse<FakeItem> builtObject1 = GetItemEnhancedResponse.<FakeItem>builder().build();

        ConsumedCapacity consumedCapacity = ConsumedCapacity.builder().capacityUnits(12.5).build();
        GetItemEnhancedResponse<FakeItem> builtObject2 = GetItemEnhancedResponse.<FakeItem>builder()
                                                                                .consumedCapacity(consumedCapacity)
                                                                                .build();

        assertThat(builtObject1.hashCode(), not(equalTo(builtObject2.hashCode())));
    }

}

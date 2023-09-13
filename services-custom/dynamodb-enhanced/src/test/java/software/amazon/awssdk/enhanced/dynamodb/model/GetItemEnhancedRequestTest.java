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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

@RunWith(MockitoJUnitRunner.class)
public class GetItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        GetItemEnhancedRequest builtObject = GetItemEnhancedRequest.builder().build();

        assertThat(builtObject.key(), is(nullValue()));
        assertThat(builtObject.consistentRead(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacity(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        Key key = Key.builder().partitionValue("key").build();

        GetItemEnhancedRequest builtObject = GetItemEnhancedRequest.builder()
                                                                   .key(key)
                                                                   .consistentRead(true)
                                                                   .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                   .build();

        assertThat(builtObject.key(), is(key));
        assertThat(builtObject.consistentRead(), is(true));
        assertThat(builtObject.returnConsumedCapacityAsString(), equalTo(ReturnConsumedCapacity.TOTAL.toString()));
        assertThat(builtObject.returnConsumedCapacity(), equalTo(ReturnConsumedCapacity.TOTAL));
    }

    @Test
    public void test_equalsAndHashCode_when_returnConsumedCapacityIsDifferent() {
        Key key = Key.builder().partitionValue("key").build();
        GetItemEnhancedRequest builtObject1 = GetItemEnhancedRequest.builder()
                                                                   .key(key)
                                                                   .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                   .build();

        GetItemEnhancedRequest builtObject2 = GetItemEnhancedRequest.builder()
                                                                    .key(key)
                                                                    .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
        assertThat(builtObject1.hashCode(), not(equalTo(builtObject2.hashCode())));
    }

    @Test
    public void test_equalsAndHashCode_when_keyIsDifferent() {
        GetItemEnhancedRequest builtObject1 = GetItemEnhancedRequest.builder()
                                                                    .key(k -> k.partitionValue("key1"))
                                                                    .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                    .build();

        GetItemEnhancedRequest builtObject2 = GetItemEnhancedRequest.builder()
                                                                    .key(k -> k.partitionValue("key2"))
                                                                    .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
        assertThat(builtObject1.hashCode(), not(equalTo(builtObject2.hashCode())));
    }

    @Test
    public void test_equalsAndHashCode_when_allValuesAreSame() {
        Key key = Key.builder().partitionValue("key").build();
        GetItemEnhancedRequest builtObject1 = GetItemEnhancedRequest.builder()
                                                                    .key(key)
                                                                    .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                                                    .consistentRead(true)
                                                                    .build();

        GetItemEnhancedRequest builtObject2 = GetItemEnhancedRequest.builder()
                                                                    .key(key)
                                                                    .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                                                    .consistentRead(true)
                                                                    .build();

        assertThat(builtObject1, equalTo(builtObject2));
        assertThat(builtObject1.hashCode(), equalTo(builtObject2.hashCode()));
    }


    @Test
    public void test_hashCode_includes_returnConsumedCapacity() {
        GetItemEnhancedRequest emptyRequest = GetItemEnhancedRequest.builder().build();
        GetItemEnhancedRequest requestWithCC1 = GetItemEnhancedRequest.builder()
                                                            .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                                            .build();
        GetItemEnhancedRequest requestWithCC2 = GetItemEnhancedRequest.builder()
                                                            .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                            .build();
        // Assert hashCode is different when returnConsumedCapacity is non-null, and all other fields are same
        assertThat(emptyRequest.hashCode(), not(equalTo(requestWithCC1.hashCode())));

        // Assert hashCode is different when returnConsumedCapacity is different, and all other fields are same
        assertThat(requestWithCC1.hashCode(), not(equalTo(requestWithCC2.hashCode())));
    }


    @Test
    public void test_returnConsumedCapacity_unknownToSdkVersion() {
        String newValue = UUID.randomUUID().toString();
        GetItemEnhancedRequest request = GetItemEnhancedRequest.builder().returnConsumedCapacity(newValue).build();

        // Assert that string getter returns the same value
        assertThat(request.returnConsumedCapacityAsString(), equalTo(newValue));

        // Assert that new value resolves to correct enum value
        assertThat(request.returnConsumedCapacity(), equalTo(ReturnConsumedCapacity.UNKNOWN_TO_SDK_VERSION));
    }


    @Test
    public void toBuilder() {
        Key key = Key.builder().partitionValue("key").build();

        GetItemEnhancedRequest builtObject = GetItemEnhancedRequest.builder()
                                                                   .key(key)
                                                                   .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                   .build();

        GetItemEnhancedRequest copiedObject = builtObject.toBuilder().build();
        assertThat(copiedObject, is(builtObject));
    }
}

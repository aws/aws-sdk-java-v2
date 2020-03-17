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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@RunWith(MockitoJUnitRunner.class)
public class GetItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        GetItemEnhancedRequest builtObject = GetItemEnhancedRequest.builder().build();

        assertThat(builtObject.key(), is(nullValue()));
        assertThat(builtObject.consistentRead(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        Key key = Key.builder().partitionValue("key").build();

        GetItemEnhancedRequest builtObject = GetItemEnhancedRequest.builder()
                                                                   .key(key)
                                                                   .consistentRead(true)
                                                                   .build();

        assertThat(builtObject.key(), is(key));
        assertThat(builtObject.consistentRead(), is(true));
    }

    @Test
    public void toBuilder() {
        Key key = Key.builder().partitionValue("key").build();

        GetItemEnhancedRequest builtObject = GetItemEnhancedRequest.builder()
                                                                   .key(key)
                                                                   .build();

        GetItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

}

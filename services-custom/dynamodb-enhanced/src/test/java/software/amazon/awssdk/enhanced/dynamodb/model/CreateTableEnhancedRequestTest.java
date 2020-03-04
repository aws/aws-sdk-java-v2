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

import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

@RunWith(MockitoJUnitRunner.class)
public class CreateTableEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        CreateTableEnhancedRequest builtObject = CreateTableEnhancedRequest.builder().build();

        assertThat(builtObject.globalSecondaryIndices(), is(nullValue()));
        assertThat(builtObject.localSecondaryIndices(), is(nullValue()));
        assertThat(builtObject.provisionedThroughput(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        GlobalSecondaryIndex globalSecondaryIndex = GlobalSecondaryIndex.create(
            "gsi_1",
            Projection.builder().projectionType(ProjectionType.ALL).build(),
            getDefaultProvisionedThroughput());

        LocalSecondaryIndex localSecondaryIndex = LocalSecondaryIndex.create(
            "lsi", Projection.builder().projectionType(ProjectionType.ALL).build());

        CreateTableEnhancedRequest builtObject = CreateTableEnhancedRequest.builder()
                                                                           .globalSecondaryIndices(globalSecondaryIndex)
                                                                           .localSecondaryIndices(localSecondaryIndex)
                                                                           .provisionedThroughput(getDefaultProvisionedThroughput())
                                                                           .build();

        assertThat(builtObject.globalSecondaryIndices(), is(Collections.singletonList(globalSecondaryIndex)));
        assertThat(builtObject.localSecondaryIndices(), is(Collections.singletonList(localSecondaryIndex)));
        assertThat(builtObject.provisionedThroughput(), is(getDefaultProvisionedThroughput()));
    }

    @Test
    public void toBuilder() {
        CreateTableEnhancedRequest builtObject = CreateTableEnhancedRequest.builder()
                                                                   .provisionedThroughput(getDefaultProvisionedThroughput())
                                                                   .build();

        CreateTableEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    private ProvisionedThroughput getDefaultProvisionedThroughput() {
        return ProvisionedThroughput.builder()
                                    .writeCapacityUnits(1L)
                                    .readCapacityUnits(2L)
                                    .build();
    }

}
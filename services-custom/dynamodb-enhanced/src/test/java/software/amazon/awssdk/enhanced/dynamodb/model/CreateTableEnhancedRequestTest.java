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

import static java.util.Arrays.asList;
import static java.util.function.Predicate.isEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
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
        assertThat(builtObject.vectorIndexes(), is(nullValue()));
        assertThat(builtObject.provisionedThroughput(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        EnhancedGlobalSecondaryIndex globalSecondaryIndex =
                EnhancedGlobalSecondaryIndex.builder()
                        .indexName("gsi_1")
                        .projection(p -> p.projectionType(ProjectionType.ALL))
                        .provisionedThroughput(getDefaultProvisionedThroughput())
                        .build();

        EnhancedLocalSecondaryIndex localSecondaryIndex = EnhancedLocalSecondaryIndex.create(
            "lsi", Projection.builder().projectionType(ProjectionType.ALL).build());

        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("embeddings-index")
                                                             .vectorAttributeName("embedding")
                                                             .dimensions(1536)
                                                             .distanceFunction(DistanceFunction.COSINE)
                                                             .projection(p -> p.projectionType(ProjectionType.ALL))
                                                             .build();

        CreateTableEnhancedRequest builtObject = CreateTableEnhancedRequest.builder()
                                                                           .globalSecondaryIndices(globalSecondaryIndex)
                                                                           .localSecondaryIndices(localSecondaryIndex)
                                                                           .vectorIndexes(vectorIndex)
                                                                           .provisionedThroughput(getDefaultProvisionedThroughput())
                                                                           .build();

        assertThat(builtObject.globalSecondaryIndices(), is(Collections.singletonList(globalSecondaryIndex)));
        assertThat(builtObject.localSecondaryIndices(), is(Collections.singletonList(localSecondaryIndex)));
        assertThat(builtObject.vectorIndexes(), is(Collections.singletonList(vectorIndex)));
        assertThat(builtObject.provisionedThroughput(), is(getDefaultProvisionedThroughput()));
    }

    @Test
    public void builder_consumerBuilder() {
        CreateTableEnhancedRequest builtObject =
            CreateTableEnhancedRequest.builder()
                                      .globalSecondaryIndices(gsi -> gsi.indexName("x"),
                                                              gsi -> gsi.indexName("y"))
                                      .localSecondaryIndices(lsi -> lsi.indexName("x"),
                                                             lsi -> lsi.indexName("y"))
                                      .provisionedThroughput(p -> p.readCapacityUnits(10L))
                                      .build();

        assertThat(builtObject.globalSecondaryIndices(),
                   equalTo(asList(EnhancedGlobalSecondaryIndex.builder().indexName("x").build(),
                                  EnhancedGlobalSecondaryIndex.builder().indexName("y").build())));
        assertThat(builtObject.localSecondaryIndices(),
                   equalTo(asList(EnhancedLocalSecondaryIndex.builder().indexName("x").build(),
                                  EnhancedLocalSecondaryIndex.builder().indexName("y").build())));
        assertThat(builtObject.provisionedThroughput(),
                   equalTo(ProvisionedThroughput.builder().readCapacityUnits(10L).build()));
    }

    @Test
    public void toBuilder() {
        CreateTableEnhancedRequest builtObject = CreateTableEnhancedRequest.builder()
                                                                   .provisionedThroughput(getDefaultProvisionedThroughput())
                                                                   .build();

        CreateTableEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void toBuilder_preservesVectorIndexes() {
        EnhancedVectorIndex vectorIndex = EnhancedVectorIndex.builder()
                                                             .indexName("my-index")
                                                             .vectorAttributeName("emb")
                                                             .dimensions(512)
                                                             .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                             .build();

        CreateTableEnhancedRequest original = CreateTableEnhancedRequest.builder()
                                                                        .vectorIndexes(vectorIndex)
                                                                        .provisionedThroughput(getDefaultProvisionedThroughput())
                                                                        .build();

        CreateTableEnhancedRequest rebuilt = original.toBuilder().build();

        assertThat(rebuilt.vectorIndexes(), is(original.vectorIndexes()));
        assertThat(rebuilt, is(original));
    }

    @Test
    public void builder_vectorIndexes_consumerOverload() {
        CreateTableEnhancedRequest request =
            CreateTableEnhancedRequest.builder()
                                      .vectorIndexes(vi -> vi.indexName("idx-a")
                                                             .vectorAttributeName("emb")
                                                             .dimensions(128)
                                                             .distanceFunction(DistanceFunction.COSINE),
                                                     vi -> vi.indexName("idx-b")
                                                             .vectorAttributeName("vec")
                                                             .dimensions(256)
                                                             .distanceFunction(DistanceFunction.EUCLIDEAN))
                                      .build();

        assertThat(request.vectorIndexes().size(), is(2));
        assertThat(request.vectorIndexes(),
                   equalTo(asList(EnhancedVectorIndex.builder().indexName("idx-a").vectorAttributeName("emb")
                                                     .dimensions(128).distanceFunction(DistanceFunction.COSINE).build(),
                                  EnhancedVectorIndex.builder().indexName("idx-b").vectorAttributeName("vec")
                                                     .dimensions(256).distanceFunction(DistanceFunction.EUCLIDEAN).build())));
    }

    @Test
    public void builder_vectorIndexes_collectionOverload() {
        EnhancedVectorIndex first = EnhancedVectorIndex.builder()
                                                       .indexName("col-idx-1")
                                                       .vectorAttributeName("emb")
                                                       .dimensions(512)
                                                       .distanceFunction(DistanceFunction.COSINE)
                                                       .build();
        EnhancedVectorIndex second = EnhancedVectorIndex.builder()
                                                        .indexName("col-idx-2")
                                                        .vectorAttributeName("vec")
                                                        .dimensions(768)
                                                        .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                        .build();

        CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                       .vectorIndexes(Arrays.asList(first, second))
                                                                       .build();

        assertThat(request.vectorIndexes().size(), is(2));
        assertThat(request.vectorIndexes(), equalTo(asList(first, second)));
    }

    private ProvisionedThroughput getDefaultProvisionedThroughput() {
        return ProvisionedThroughput.builder()
                                    .writeCapacityUnits(1L)
                                    .readCapacityUnits(2L)
                                    .build();
    }

}

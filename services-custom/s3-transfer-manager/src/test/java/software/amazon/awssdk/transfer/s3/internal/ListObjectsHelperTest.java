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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

class ListObjectsHelperTest {
    private Function<ListObjectsV2Request,
        CompletableFuture<ListObjectsV2Response>> listObjectsFunction;
    private ListObjectsHelper listObjectsHelper;

    @BeforeEach
    public void setup() {
        listObjectsFunction = Mockito.mock(Function.class);
        listObjectsHelper = new ListObjectsHelper(listObjectsFunction);
    }

    @Test
    void listS3Objects_noNextPageOrCommonPrefixes_shouldNotFetchAgain() {
        ListObjectsV2Response response = listObjectsV2Response("key1", "key2");
        CompletableFuture<ListObjectsV2Response> future = CompletableFuture.completedFuture(response);
        when(listObjectsFunction.apply(any(ListObjectsV2Request.class)))
            .thenReturn(future);

        List<S3Object> actualObjects = new ArrayList<>();

        listObjectsHelper.listS3ObjectsRecursively(ListObjectsV2Request.builder()
                                                                       .bucket("bucket")
                                                                       .build())
                         .subscribe(actualObjects::add).join();


        future.complete(response);
        assertThat(actualObjects).hasSameElementsAs(response.contents());
        verify(listObjectsFunction, times(1)).apply(any(ListObjectsV2Request.class));
    }

    /**
     *              source
     *    /    /   | |      \        \
     *   1    2   3  4      jan       feb
     *                   / /  | \     /  \
     *                  1 2    3  4   1    2
     *  The order of S3Objects should be "1, 2, 3, 4, jan/1, jan/2, jan/3, jan/4, feb/1, feb/2"
     */
    @Test
    void listS3Objects_hasNextPageAndCommonPrefixes_shouldReturnAll() {
        List<CommonPrefix> commonPrefixes = Arrays.asList(CommonPrefix.builder().prefix("jan/").build(),
                                                          CommonPrefix.builder().prefix("feb/").build());

        ListObjectsV2Response responsePage1 = listObjectsV2Response("nextPage", commonPrefixes, "1", "2");

        ListObjectsV2Response responsePage2 = listObjectsV2Response(null, Collections.emptyList(), "3", "4");

        ListObjectsV2Response responsePage3 = listObjectsV2Response("nextPage", Collections.emptyList(), "jan/1", "jan/2");
        ListObjectsV2Response responsePage4 = listObjectsV2Response(null, Collections.emptyList(), "jan/3", "jan/4");
        ListObjectsV2Response responsePage5 = listObjectsV2Response(null, Collections.emptyList(), "feb/1", "feb/2");

        CompletableFuture<ListObjectsV2Response> futurePage1 = CompletableFuture.completedFuture(responsePage1);
        CompletableFuture<ListObjectsV2Response> futurePage2 = CompletableFuture.completedFuture(responsePage2);
        CompletableFuture<ListObjectsV2Response> futurePage3 = CompletableFuture.completedFuture(responsePage3);
        CompletableFuture<ListObjectsV2Response> futurePage4 = CompletableFuture.completedFuture(responsePage4);
        CompletableFuture<ListObjectsV2Response> futurePage5 = CompletableFuture.completedFuture(responsePage5);

        when(listObjectsFunction.apply(any(ListObjectsV2Request.class))).thenReturn(futurePage1)
                                                                        .thenReturn(futurePage2)
                                                                        .thenReturn(futurePage3)
                                                                        .thenReturn(futurePage4)
                                                                        .thenReturn(futurePage5);
        List<S3Object> actualObjects = new ArrayList<>();

        ListObjectsV2Request firstRequest = ListObjectsV2Request.builder()
                                                                .bucket("bucket")
                                                                .build();
        listObjectsHelper.listS3ObjectsRecursively(firstRequest)
                         .subscribe(actualObjects::add).join();


        futurePage1.complete(responsePage1);
        ArgumentCaptor<ListObjectsV2Request> argumentCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(listObjectsFunction, times(5)).apply(argumentCaptor.capture());
        List<ListObjectsV2Request> actualListObjectsV2Request = argumentCaptor.getAllValues();

        assertThat(actualListObjectsV2Request).hasSize(5)
                                              .satisfies(list -> {
                                                  assertThat(list.get(0)).isEqualTo(firstRequest);
                                                  assertThat(list.get(1)).isEqualTo(firstRequest.toBuilder().continuationToken(
                                                      "nextPage").build());
                                                  assertThat(list.get(2)).isEqualTo(firstRequest.toBuilder().prefix("jan/").build());
                                                  assertThat(list.get(3)).isEqualTo(firstRequest.toBuilder().prefix("jan/")
                                                                                                .continuationToken("nextPage").build());
                                                  assertThat(list.get(4)).isEqualTo(firstRequest.toBuilder().prefix("feb/").build());
                                              });
        assertThat(actualObjects).hasSize(10);
    }

    private ListObjectsV2Response listObjectsV2Response(String... keys) {
        return listObjectsV2Response(null, null, keys);
    }

    private ListObjectsV2Response listObjectsV2Response(String continuationToken,
                                                        List<CommonPrefix> commonPrefixes,
                                                        String... keys) {
        List<S3Object> s3Objects = Arrays.stream(keys).map(k -> S3Object.builder().key(k).build()).collect(Collectors.toList());
        return ListObjectsV2Response.builder()
                                    .nextContinuationToken(continuationToken)
                                    .commonPrefixes(commonPrefixes)
                                    .contents(s3Objects)
                                    .build();
    }
}

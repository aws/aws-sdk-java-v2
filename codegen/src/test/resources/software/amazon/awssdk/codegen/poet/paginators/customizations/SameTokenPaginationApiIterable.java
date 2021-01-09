package software.amazon.awssdk.services.jsonprotocoltests.paginators;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.pagination.sync.PaginatedItemsIterable;
import software.amazon.awssdk.core.pagination.sync.PaginatedResponsesIterator;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.pagination.sync.SyncPageFetcher;
import software.amazon.awssdk.core.util.PaginatorUtils;
import software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsClient;
import software.amazon.awssdk.services.jsonprotocoltests.model.SameTokenPaginationApiRequest;
import software.amazon.awssdk.services.jsonprotocoltests.model.SameTokenPaginationApiResponse;
import software.amazon.awssdk.services.jsonprotocoltests.model.SimpleStruct;

/**
 * <p>
 * Represents the output for the
 * {@link software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsClient#sameTokenPaginationApiPaginator(software.amazon.awssdk.services.jsonprotocoltests.model.SameTokenPaginationApiRequest)}
 * operation which is a paginated operation. This class is an iterable of
 * {@link software.amazon.awssdk.services.jsonprotocoltests.model.SameTokenPaginationApiResponse} that can be used to
 * iterate through all the response pages of the operation.
 * </p>
 * <p>
 * When the operation is called, an instance of this class is returned. At this point, no service calls are made yet and
 * so there is no guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily
 * loading response pages by making service calls until there are no pages left or your iteration stops. If there are
 * errors in your request, you will see the failures only after you start iterating through the iterable.
 * </p>
 *
 * <p>
 * The following are few ways to iterate through the response pages:
 * </p>
 * 1) Using a Stream
 *
 * <pre>
 * {@code
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.SameTokenPaginationApiIterable responses = client.sameTokenPaginationApiPaginator(request);
 * responses.stream().forEach(....);
 * }
 * </pre>
 *
 * 2) Using For loop
 *
 * <pre>
 * {
 *     &#064;code
 *     software.amazon.awssdk.services.jsonprotocoltests.paginators.SameTokenPaginationApiIterable responses = client
 *             .sameTokenPaginationApiPaginator(request);
 *     for (software.amazon.awssdk.services.jsonprotocoltests.model.SameTokenPaginationApiResponse response : responses) {
 *         // do something;
 *     }
 * }
 * </pre>
 *
 * 3) Use iterator directly
 *
 * <pre>
 * {@code
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.SameTokenPaginationApiIterable responses = client.sameTokenPaginationApiPaginator(request);
 * responses.iterator().forEachRemaining(....);
 * }
 * </pre>
 * <p>
 * <b>Please notice that the configuration of MaxResults won't limit the number of results you get with the paginator.
 * It only limits the number of results in each page.</b>
 * </p>
 * <p>
 * <b>Note: If you prefer to have control on service calls, use the
 * {@link #sameTokenPaginationApi(software.amazon.awssdk.services.jsonprotocoltests.model.SameTokenPaginationApiRequest)}
 * operation.</b>
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public class SameTokenPaginationApiIterable implements SdkIterable<SameTokenPaginationApiResponse> {
    private final JsonProtocolTestsClient client;

    private final SameTokenPaginationApiRequest firstRequest;

    public SameTokenPaginationApiIterable(JsonProtocolTestsClient client, SameTokenPaginationApiRequest firstRequest) {
        this.client = client;
        this.firstRequest = firstRequest;
    }

    @Override
    public Iterator<SameTokenPaginationApiResponse> iterator() {
        return PaginatedResponsesIterator.builder().nextPageFetcher(new SameTokenPaginationApiResponseFetcher()).build();
    }

    /**
     * Returns an iterable to iterate through the paginated {@link SameTokenPaginationApiResponse#items()} member. The
     * returned iterable is used to iterate through the results across all response pages and not a single page.
     *
     * This method is useful if you are interested in iterating over the paginated member in the response pages instead
     * of the top level pages. Similar to iteration over pages, this method internally makes service calls to get the
     * next list of results until the iteration stops or there are no more results.
     */
    public final SdkIterable<SimpleStruct> items() {
        Function<SameTokenPaginationApiResponse, Iterator<SimpleStruct>> getIterator = response -> {
            if (response != null && response.items() != null) {
                return response.items().iterator();
            }
            return Collections.emptyIterator();
        };
        return PaginatedItemsIterable.<SameTokenPaginationApiResponse, SimpleStruct> builder().pagesIterable(this)
                                                                                              .itemIteratorFunction(getIterator).build();
    }

    private class SameTokenPaginationApiResponseFetcher implements SyncPageFetcher<SameTokenPaginationApiResponse> {
        private Object lastToken;

        @Override
        public boolean hasNextPage(SameTokenPaginationApiResponse previousPage) {
            return PaginatorUtils.isOutputTokenAvailable(previousPage.nextToken()) && !previousPage.nextToken().equals(lastToken);
        }

        @Override
        public SameTokenPaginationApiResponse nextPage(SameTokenPaginationApiResponse previousPage) {
            if (previousPage == null) {
                lastToken = null;
                return client.sameTokenPaginationApi(firstRequest);
            }
            lastToken = previousPage.nextToken();
            return client.sameTokenPaginationApi(firstRequest.toBuilder().nextToken(previousPage.nextToken()).build());
        }
    }
}

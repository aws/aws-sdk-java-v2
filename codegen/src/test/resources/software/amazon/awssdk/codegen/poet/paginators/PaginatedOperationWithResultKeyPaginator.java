package software.amazon.awssdk.services.jsonprotocoltests.paginators;

import java.util.Iterator;
import java.util.function.Function;
import javax.annotation.Generated;
import software.amazon.awssdk.core.pagination.NextPageFetcher;
import software.amazon.awssdk.core.pagination.PaginatedItemsIterable;
import software.amazon.awssdk.core.pagination.PaginatedResponsesIterator;
import software.amazon.awssdk.core.pagination.SdkIterable;
import software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsClient;
import software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithResultKeyRequest;
import software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.jsonprotocoltests.model.SimpleStruct;

/**
 * <p>
 * Represents the output for the
 * {@link software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsClient#paginatedOperationWithResultKeyIterable(software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithResultKeyRequest)}
 * operation which is a paginated operation. This class is an iterable of
 * {@link software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithResultKeyResponse} that can be
 * used to iterate through all the response pages of the operation.
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
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithResultKeyPaginator responses = client.paginatedOperationWithResultKeyIterable(request);
 * responses.stream().forEach(....);
 * }
 * </pre>
 *
 * 2) Using For loop
 *
 * <pre>
 * {
 *     &#064;code
 *     software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithResultKeyPaginator responses = client
 *             .paginatedOperationWithResultKeyIterable(request);
 *     for (software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithResultKeyResponse response : responses) {
 *         // do something;
 *     }
 * }
 * </pre>
 *
 * 3) Use iterator directly
 *
 * <pre>
 * {@code
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithResultKeyPaginator responses = client.paginatedOperationWithResultKeyIterable(request);
 * responses.iterator().forEachRemaining(....);
 * }
 * </pre>
 * <p>
 * <b>Note: If you prefer to have control on service calls, use the
 * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithResultKeyRequest)}
 * operation.</b>
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class PaginatedOperationWithResultKeyPaginator implements SdkIterable<PaginatedOperationWithResultKeyResponse> {
    private final JsonProtocolTestsClient client;

    private final PaginatedOperationWithResultKeyRequest firstRequest;

    private final NextPageFetcher nextPageFetcher;

    public PaginatedOperationWithResultKeyPaginator(final JsonProtocolTestsClient client,
                                                    final PaginatedOperationWithResultKeyRequest firstRequest) {
        this.client = client;
        this.firstRequest = firstRequest;
        this.nextPageFetcher = new PaginatedOperationWithResultKeyResponseFetcher();
    }

    @Override
    public Iterator<PaginatedOperationWithResultKeyResponse> iterator() {
        return new PaginatedResponsesIterator(nextPageFetcher);
    }

    /**
     * Returns an iterable to iterate through the paginated {@link PaginatedOperationWithResultKeyResponse#items()}
     * member. The returned iterable is used to iterate through the results across all response pages and not a single
     * page.
     *
     * This method is useful if you are interested in iterating over the paginated member in the response pages instead
     * of the top level pages. Similar to iteration over pages, this method internally makes service calls to get the
     * next list of results until the iteration stops or there are no more results.
     */
    public SdkIterable<SimpleStruct> items() {
        Function<PaginatedOperationWithResultKeyResponse, Iterator<SimpleStruct>> getIterator = response -> response != null ? response
            .items().iterator() : null;

        return new PaginatedItemsIterable(this, getIterator);
    }

    private class PaginatedOperationWithResultKeyResponseFetcher implements
                                                                 NextPageFetcher<PaginatedOperationWithResultKeyResponse> {
        @Override
        public boolean hasNextPage(PaginatedOperationWithResultKeyResponse previousPage) {
            return previousPage.nextToken() != null;
        }

        @Override
        public PaginatedOperationWithResultKeyResponse nextPage(PaginatedOperationWithResultKeyResponse previousPage) {
            if (previousPage == null) {
                return client.paginatedOperationWithResultKey(firstRequest);
            }
            return client.paginatedOperationWithResultKey(firstRequest.toBuilder().nextToken(previousPage.nextToken()).build());
        }
    }
}

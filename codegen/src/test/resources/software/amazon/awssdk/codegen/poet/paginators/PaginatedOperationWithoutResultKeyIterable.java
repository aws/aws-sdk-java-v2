package software.amazon.awssdk.services.jsonprotocoltests.paginators;

import java.util.Collections;
import java.util.Iterator;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.pagination.sync.PaginatedResponsesIterator;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.pagination.sync.SyncPageFetcher;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsClient;
import software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyResponse;

/**
 * <p>
 * Represents the output for the
 * {@link software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsClient#paginatedOperationWithoutResultKeyPaginator(software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyRequest)}
 * operation which is a paginated operation. This class is an iterable of
 * {@link software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyResponse} that can
 * be used to iterate through all the response pages of the operation.
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
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithoutResultKeyIterable responses = client.paginatedOperationWithoutResultKeyPaginator(request);
 * responses.stream().forEach(....);
 * }
 * </pre>
 *
 * 2) Using For loop
 *
 * <pre>
 * {
 *     &#064;code
 *     software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithoutResultKeyIterable responses = client
 *             .paginatedOperationWithoutResultKeyPaginator(request);
 *     for (software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyResponse response : responses) {
 *         // do something;
 *     }
 * }
 * </pre>
 *
 * 3) Use iterator directly
 *
 * <pre>
 * {@code
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithoutResultKeyIterable responses = client.paginatedOperationWithoutResultKeyPaginator(request);
 * responses.iterator().forEachRemaining(....);
 * }
 * </pre>
 * <p>
 * <b>Note: If you prefer to have control on service calls, use the
 * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyRequest)}
 * operation.</b>
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public class PaginatedOperationWithoutResultKeyIterable implements SdkIterable<PaginatedOperationWithoutResultKeyResponse> {
    private final JsonProtocolTestsClient client;

    private final PaginatedOperationWithoutResultKeyRequest firstRequest;

    private final SyncPageFetcher nextPageFetcher;

    public PaginatedOperationWithoutResultKeyIterable(JsonProtocolTestsClient client,
            PaginatedOperationWithoutResultKeyRequest firstRequest) {
        this.client = client;
        this.firstRequest = firstRequest;
        this.nextPageFetcher = new PaginatedOperationWithoutResultKeyResponseFetcher();
    }

    @Override
    public Iterator<PaginatedOperationWithoutResultKeyResponse> iterator() {
        return PaginatedResponsesIterator.builder().nextPageFetcher(nextPageFetcher).build();
    }

    /**
     * <p>
     * A helper method to resume the pages in case of unexpected failures. The method takes the last successful response
     * page as input and returns an instance of {@link PaginatedOperationWithoutResultKeyIterable} that can be used to
     * retrieve the consecutive pages that follows the input page.
     * </p>
     */
    private final PaginatedOperationWithoutResultKeyIterable resume(PaginatedOperationWithoutResultKeyResponse lastSuccessfulPage) {
        if (nextPageFetcher.hasNextPage(lastSuccessfulPage)) {
            return new PaginatedOperationWithoutResultKeyIterable(client, firstRequest.toBuilder()
                    .nextToken(lastSuccessfulPage.nextToken()).build());
        }
        return new PaginatedOperationWithoutResultKeyIterable(client, firstRequest) {
            @Override
            public Iterator<PaginatedOperationWithoutResultKeyResponse> iterator() {
                return Collections.emptyIterator();
            }
        };
    }

    private class PaginatedOperationWithoutResultKeyResponseFetcher implements
            SyncPageFetcher<PaginatedOperationWithoutResultKeyResponse> {
        @Override
        public boolean hasNextPage(PaginatedOperationWithoutResultKeyResponse previousPage) {
            return previousPage.nextToken() != null && !SdkAutoConstructList.class.isInstance(previousPage.nextToken())
                    && !SdkAutoConstructMap.class.isInstance(previousPage.nextToken());
        }

        @Override
        public PaginatedOperationWithoutResultKeyResponse nextPage(PaginatedOperationWithoutResultKeyResponse previousPage) {
            if (previousPage == null) {
                return client.paginatedOperationWithoutResultKey(firstRequest);
            }
            return client
                    .paginatedOperationWithoutResultKey(firstRequest.toBuilder().nextToken(previousPage.nextToken()).build());
        }
    }
}

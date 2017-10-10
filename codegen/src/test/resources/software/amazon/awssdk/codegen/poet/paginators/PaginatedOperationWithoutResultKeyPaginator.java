package software.amazon.awssdk.services.jsonprotocoltests.paginators;

import java.util.Iterator;
import javax.annotation.Generated;
import software.amazon.awssdk.core.pagination.NextPageFetcher;
import software.amazon.awssdk.core.pagination.PaginatedResponsesIterator;
import software.amazon.awssdk.core.pagination.SdkIterable;
import software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsClient;
import software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyResponse;

/**
 * <p>
 * Represents the output for the
 * {@link software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsClient#paginatedOperationWithoutResultKeyIterable(software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyRequest)}
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
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithoutResultKeyPaginator responses = client.paginatedOperationWithoutResultKeyIterable(request);
 * responses.stream().forEach(....);
 * }
 * </pre>
 *
 * 2) Using For loop
 *
 * <pre>
 * {
 *     &#064;code
 *     software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithoutResultKeyPaginator responses = client
 *             .paginatedOperationWithoutResultKeyIterable(request);
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
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithoutResultKeyPaginator responses = client.paginatedOperationWithoutResultKeyIterable(request);
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
public final class PaginatedOperationWithoutResultKeyPaginator implements SdkIterable<PaginatedOperationWithoutResultKeyResponse> {
    private final JsonProtocolTestsClient client;

    private final PaginatedOperationWithoutResultKeyRequest firstRequest;

    private final NextPageFetcher nextPageFetcher;

    public PaginatedOperationWithoutResultKeyPaginator(final JsonProtocolTestsClient client,
                                                       final PaginatedOperationWithoutResultKeyRequest firstRequest) {
        this.client = client;
        this.firstRequest = firstRequest;
        this.nextPageFetcher = new PaginatedOperationWithoutResultKeyResponseFetcher();
    }

    @Override
    public Iterator<PaginatedOperationWithoutResultKeyResponse> iterator() {
        return new PaginatedResponsesIterator(nextPageFetcher);
    }

    private class PaginatedOperationWithoutResultKeyResponseFetcher implements
                                                                    NextPageFetcher<PaginatedOperationWithoutResultKeyResponse> {
        @Override
        public boolean hasNextPage(PaginatedOperationWithoutResultKeyResponse previousPage) {
            return previousPage.nextToken() != null;
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

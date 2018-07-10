package software.amazon.awssdk.services.jsonprotocoltests.paginators;

import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
<<<<<<< HEAD
import software.amazon.awssdk.core.async.SdkPublisher;
=======
import software.amazon.awssdk.annotations.Generated;
>>>>>>> public/master
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.EmptySubscription;
import software.amazon.awssdk.core.pagination.async.ResponsesSubscription;
import software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsAsyncClient;
import software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyResponse;

/**
 * <p>
 * Represents the output for the
 * {@link software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsAsyncClient#paginatedOperationWithoutResultKeyPaginator(software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyRequest)}
 * operation which is a paginated operation. This class is a type of {@link org.reactivestreams.Publisher} which can be
 * used to provide a sequence of
 * {@link software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyResponse} response
 * pages as per demand from the subscriber.
 * </p>
 * <p>
 * When the operation is called, an instance of this class is returned. At this point, no service calls are made yet and
 * so there is no guarantee that the request is valid. If there are errors in your request, you will see the failures
 * only after you start streaming the data. The subscribe method should be called as a request to start streaming data.
 * For more info, see {@link org.reactivestreams.Publisher#subscribe(org.reactivestreams.Subscriber)}. Each call to the
 * subscribe method will result in a new {@link org.reactivestreams.Subscription} i.e., a new contract to stream data
 * from the starting request.
 * </p>
 *
 * <p>
 * The following are few ways to use the response class:
 * </p>
 * 1) Using the forEach helper method
 *
 * <pre>
 * {@code
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithoutResultKeyPublisher publisher = client.paginatedOperationWithoutResultKeyPaginator(request);
 * CompletableFuture<Void> future = publisher.forEach(res -> { // Do something with the response });
 * future.get();
 * }
 * </pre>
 *
 * 2) Using a custom subscriber
 *
 * <pre>
 * {@code
 * software.amazon.awssdk.services.jsonprotocoltests.paginators.PaginatedOperationWithoutResultKeyPublisher publisher = client.paginatedOperationWithoutResultKeyPaginator(request);
 * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyResponse>() {
 *
 * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
 *
 *
 * public void onNext(software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyResponse response) { //... };
 * });}
 * </pre>
 *
 * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
 * <p>
 * <b>Note: If you prefer to have control on service calls, use the
 * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.jsonprotocoltests.model.PaginatedOperationWithoutResultKeyRequest)}
 * operation.</b>
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public class PaginatedOperationWithoutResultKeyPublisher implements SdkPublisher<PaginatedOperationWithoutResultKeyResponse> {
    private final JsonProtocolTestsAsyncClient client;

    private final PaginatedOperationWithoutResultKeyRequest firstRequest;

    private final AsyncPageFetcher nextPageFetcher;

    private boolean isLastPage;

    public PaginatedOperationWithoutResultKeyPublisher(JsonProtocolTestsAsyncClient client,
                                                       PaginatedOperationWithoutResultKeyRequest firstRequest) {
        this(client, firstRequest, false);
    }

    private PaginatedOperationWithoutResultKeyPublisher(JsonProtocolTestsAsyncClient client,
                                                        PaginatedOperationWithoutResultKeyRequest firstRequest, boolean isLastPage) {
        this.client = client;
        this.firstRequest = firstRequest;
        this.isLastPage = isLastPage;
        this.nextPageFetcher = new PaginatedOperationWithoutResultKeyResponseFetcher();
    }

    @Override
    public void subscribe(Subscriber<? super PaginatedOperationWithoutResultKeyResponse> subscriber) {
        subscriber.onSubscribe(ResponsesSubscription.builder().subscriber(subscriber).nextPageFetcher(nextPageFetcher).build());
    }

    /**
     * <p>
     * A helper method to resume the pages in case of unexpected failures. The method takes the last successful response
     * page as input and returns an instance of {@link PaginatedOperationWithoutResultKeyPublisher} that can be used to
     * retrieve the consecutive pages that follows the input page.
     * </p>
     */
    private final PaginatedOperationWithoutResultKeyPublisher resume(PaginatedOperationWithoutResultKeyResponse lastSuccessfulPage) {
        if (nextPageFetcher.hasNextPage(lastSuccessfulPage)) {
            return new PaginatedOperationWithoutResultKeyPublisher(client, firstRequest.toBuilder()
                                                                                       .nextToken(lastSuccessfulPage.nextToken()).build());
        }
        return new PaginatedOperationWithoutResultKeyPublisher(client, firstRequest, true) {
            @Override
            public void subscribe(Subscriber<? super PaginatedOperationWithoutResultKeyResponse> subscriber) {
                subscriber.onSubscribe(new EmptySubscription(subscriber));
            }
        };
    }

    private class PaginatedOperationWithoutResultKeyResponseFetcher implements
                                                                    AsyncPageFetcher<PaginatedOperationWithoutResultKeyResponse> {
        @Override
        public boolean hasNextPage(final PaginatedOperationWithoutResultKeyResponse previousPage) {
            return previousPage.nextToken() != null;
        }

        @Override
        public CompletableFuture<PaginatedOperationWithoutResultKeyResponse> nextPage(
            final PaginatedOperationWithoutResultKeyResponse previousPage) {
            if (previousPage == null) {
                return client.paginatedOperationWithoutResultKey(firstRequest);
            }
            return client
                .paginatedOperationWithoutResultKey(firstRequest.toBuilder().nextToken(previousPage.nextToken()).build());
        }
    }
}

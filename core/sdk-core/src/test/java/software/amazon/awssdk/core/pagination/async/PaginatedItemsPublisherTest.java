package software.amazon.awssdk.core.pagination.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.SdkResponse;

public class PaginatedItemsPublisherTest {
    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void subscribe_largePage_doesNotFail() throws Exception {
        int nItems = 100_000;

        Function<SdkResponse, Iterator<String>> iteratorFn = resp ->
            new Iterator<String>() {
                private int count = 0;

                @Override
                public boolean hasNext() {
                    return count < nItems;
                }

                @Override
                public String next() {
                    ++count;
                    return "item";
                }
            };

        AsyncPageFetcher<SdkResponse> pageFetcher = new AsyncPageFetcher<SdkResponse>() {
            @Override
            public boolean hasNextPage(SdkResponse oldPage) {
                return false;
            }

            @Override
            public CompletableFuture<SdkResponse> nextPage(SdkResponse oldPage) {
                return CompletableFuture.completedFuture(mock(SdkResponse.class));
            }
        };

        PaginatedItemsPublisher<SdkResponse, String> publisher = PaginatedItemsPublisher.builder()
                                                                                        .isLastPage(false)
                                                                                        .nextPageFetcher(pageFetcher)
                                                                                        .iteratorFunction(iteratorFn)
                                                                                        .build();

        AtomicLong counter = new AtomicLong();
        publisher.subscribe(i -> counter.incrementAndGet()).join();
        assertThat(counter.get()).isEqualTo(nItems);
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void subscribe_longStream_doesNotFail() throws Exception {
        int nPages = 100_000;
        int nItemsPerPage = 1;
        Function<SdkResponse, Iterator<String>> iteratorFn = resp ->
            new Iterator<String>() {
                private int count = 0;

                @Override
                public boolean hasNext() {
                    return count < nItemsPerPage;
                }

                @Override
                public String next() {
                    ++count;
                    return "item";
                }
            };

        AsyncPageFetcher<TestResponse> pageFetcher = new AsyncPageFetcher<TestResponse>() {
            @Override
            public boolean hasNextPage(TestResponse oldPage) {
                return oldPage.pageNumber() < nPages - 1;
            }

            @Override
            public CompletableFuture<TestResponse> nextPage(TestResponse oldPage) {
                int nextPageNum;
                if (oldPage == null) {
                    nextPageNum = 0;
                } else {
                    nextPageNum = oldPage.pageNumber() + 1;
                }
                return CompletableFuture.completedFuture(createResponse(nextPageNum));
            }
        };

        PaginatedItemsPublisher<SdkResponse, String> publisher = PaginatedItemsPublisher.builder()
                                                                                        .isLastPage(false)
                                                                                        .nextPageFetcher(pageFetcher)
                                                                                        .iteratorFunction(iteratorFn)
                                                                                        .build();

        AtomicLong counter = new AtomicLong();
        publisher.subscribe(i -> counter.incrementAndGet()).join();
        assertThat(counter.get()).isEqualTo(nPages * nItemsPerPage);
    }

    private abstract class TestResponse extends SdkResponse {

        protected TestResponse(Builder builder) {
            super(builder);
        }

        abstract Integer pageNumber();
    }

    private static TestResponse createResponse(Integer pageNumber) {
        TestResponse mock = mock(TestResponse.class);
        when(mock.pageNumber()).thenReturn(pageNumber);
        return mock;
    }
}

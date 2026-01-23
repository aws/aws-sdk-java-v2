package software.amazon.awssdk.services.customresponsemetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.PaginatedOperationWithMoreResultsRequest;
import software.amazon.awssdk.services.protocolrestjson.model.PaginatedOperationWithMoreResultsResponse;
import software.amazon.awssdk.services.protocolrestjson.paginators.PaginatedOperationWithMoreResultsIterable;

@ExtendWith(MockitoExtension.class)
public class MoreResultsIterableTest {

    @Mock
    private ProtocolRestJsonClient client;

    @Test
    public void paginatedOperationWithMoreResultsIterable_withNullTruncatedFieldResponse_doesNotThrowNullPointerException() {
        when(client.paginatedOperationWithMoreResultsPaginator(any(PaginatedOperationWithMoreResultsRequest.class)))
            .thenReturn(new PaginatedOperationWithMoreResultsIterable(client, PaginatedOperationWithMoreResultsRequest.builder().build()));
        when(client.paginatedOperationWithMoreResults(any(PaginatedOperationWithMoreResultsRequest.class)))
            .thenReturn(PaginatedOperationWithMoreResultsResponse.builder().build());

        PaginatedOperationWithMoreResultsRequest requestWithoutTruncatedSet = PaginatedOperationWithMoreResultsRequest.builder().build();
        PaginatedOperationWithMoreResultsIterable pages = client.paginatedOperationWithMoreResultsPaginator(requestWithoutTruncatedSet);

        pages.forEach(r -> {
            assertThat(r.truncated()).isNull();
        });
    }
}

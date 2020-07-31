package software.amazon.awssdk.core.internal.http.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import org.junit.Test;

public class AsyncResponseHandlerTest {
    @Test
    public void onErrorAfterPrepareForwardsErrorToFuture() {
        AsyncResponseHandler<Object> responseHandler = new AsyncResponseHandler<>(null, null, null);
        CompletableFuture<Object> future = responseHandler.prepare();

        assertThat(future).isNotCompleted();

        Throwable t = new Throwable();
        responseHandler.onError(t);

        assertThat(future).hasFailedWithThrowableThat().isEqualTo(t);
    }

    @Test
    public void onErrorBeforePrepareStoresExceptionForPrepare() {
        AsyncResponseHandler<Object> responseHandler = new AsyncResponseHandler<>(null, null, null);

        Throwable t = new Throwable();
        responseHandler.onError(t);

        assertThat(responseHandler.prepare()).hasFailedWithThrowableThat().isEqualTo(t);
    }
}
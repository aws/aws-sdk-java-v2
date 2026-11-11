package software.amazon.awssdk.awscore.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.interceptor.DefaultFailedExecutionContext;

public class HelpfulUnknownHostExceptionInterceptorTest {
    private static final ExecutionInterceptor INTERCEPTOR = new HelpfulUnknownHostExceptionInterceptor();

    @Test
    public void modifyException_skipsNonUnknownHostExceptions() {
        IOException exception = new IOException();
        assertThat(modifyException(exception)).isEqualTo(exception);
    }

    @Test
    public void modifyException_supportsNestedUnknownHostExceptions() {
        Exception exception = new UnknownHostException();
        exception.initCause(new IOException());
        exception = new IllegalArgumentException(exception);
        exception = new UnsupportedOperationException(exception);

        assertThat(modifyException(exception)).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void modifyException_returnsNetworkHelp_forUnknownHostException() {
        UnknownHostException exception = new UnknownHostException();
        assertThat(modifyException(exception))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("network");
    }

    @Test
    public void modifyException_preservesOriginalExceptionAsCause() {
        UnknownHostException exception = new UnknownHostException("iam.us-east-1.amazonaws.com");
        Throwable result = modifyException(exception);
        assertThat(result).isInstanceOf(SdkClientException.class);
        assertThat(result.getCause()).isSameAs(exception);
    }

    private Throwable modifyException(Throwable throwable) {
        SdkRequest sdkRequest = Mockito.mock(SdkRequest.class);

        DefaultFailedExecutionContext context =
            DefaultFailedExecutionContext.builder()
                                         .interceptorContext(InterceptorContext.builder().request(sdkRequest).build())
                                         .exception(throwable)
                                         .build();

        return INTERCEPTOR.modifyException(context, new ExecutionAttributes());
    }
}
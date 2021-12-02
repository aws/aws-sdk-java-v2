package software.amazon.awssdk.awscore.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.UnknownHostException;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.interceptor.DefaultFailedExecutionContext;
import software.amazon.awssdk.regions.Region;

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

        assertThat(modifyException(exception, Region.AWS_GLOBAL)).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void modifyException_returnsGenericHelp_forGlobalRegions() {
        UnknownHostException exception = new UnknownHostException();
        assertThat(modifyException(exception, Region.AWS_GLOBAL))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("network");
    }

    @Test
    public void modifyException_returnsGenericHelp_forUnknownServices() {
        UnknownHostException exception = new UnknownHostException();
        assertThat(modifyException(exception, Region.US_EAST_1, "millems-hotdog-stand"))
            .isInstanceOf(SdkClientException.class)
            .satisfies(t -> doesNotHaveMessageContaining(t, "global"))
            .hasMessageContaining("network");
    }

    @Test
    public void modifyException_returnsGenericHelp_forUnknownServicesInUnknownRegions() {
        UnknownHostException exception = new UnknownHostException();
        assertThat(modifyException(exception, Region.of("cn-north-99"), "millems-hotdog-stand"))
            .isInstanceOf(SdkClientException.class)
            .satisfies(t -> doesNotHaveMessageContaining(t, "global"))
            .hasMessageContaining("network");
    }

    @Test
    public void modifyException_returnsGenericHelp_forServicesRegionalizedInAllPartitions() {
        UnknownHostException exception = new UnknownHostException();
        assertThat(modifyException(exception, Region.US_EAST_1, "dynamodb"))
            .isInstanceOf(SdkClientException.class)
            .satisfies(t -> doesNotHaveMessageContaining(t, "global"))
            .hasMessageContaining("network");
    }

    @Test
    public void modifyException_returnsGenericGlobalRegionHelp_forServicesGlobalInSomePartitionOtherThanTheClientPartition() {
        UnknownHostException exception = new UnknownHostException();
        assertThat(modifyException(exception, Region.of("cn-north-99"), "iam"))
            .isInstanceOf(SdkClientException.class)
            .satisfies(t -> doesNotHaveMessageContaining(t, "network"))
            .hasMessageContaining("aws-global")
            .hasMessageContaining("aws-cn-global");
    }

    @Test
    public void modifyException_returnsSpecificGlobalRegionHelp_forServicesGlobalInTheClientRegionPartition() {
        UnknownHostException exception = new UnknownHostException();
        assertThat(modifyException(exception, Region.of("cn-north-1"), "iam"))
            .isInstanceOf(SdkClientException.class)
            .satisfies(t -> doesNotHaveMessageContaining(t, "aws-global"))
            .hasMessageContaining("aws-cn-global");
    }

    private void doesNotHaveMessageContaining(Throwable throwable, String value) {
        assertThat(throwable.getMessage()).doesNotContain(value);
    }

    private Throwable modifyException(Throwable throwable) {
        return modifyException(throwable, null);
    }

    private Throwable modifyException(Throwable throwable, Region clientRegion) {
        return modifyException(throwable, clientRegion, null);
    }

    private Throwable modifyException(Throwable throwable, Region clientRegion, String serviceEndpointPrefix) {
        SdkRequest sdkRequest = Mockito.mock(SdkRequest.class);

        DefaultFailedExecutionContext context =
            DefaultFailedExecutionContext.builder()
                                         .interceptorContext(InterceptorContext.builder().request(sdkRequest).build())
                                         .exception(throwable)
                                         .build();

        ExecutionAttributes executionAttributes =
            new ExecutionAttributes().putAttribute(AwsExecutionAttribute.AWS_REGION, clientRegion)
                                     .putAttribute(AwsExecutionAttribute.ENDPOINT_PREFIX, serviceEndpointPrefix);

        return INTERCEPTOR.modifyException(context, executionAttributes);
    }
}
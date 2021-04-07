package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import java.util.concurrent.CompletionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;

public class ExecutionAttributesTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void syncClient_disableHostPrefixInjection_isPresent() {
        ExecutionInterceptor interceptor = mock(ExecutionInterceptor.class);
        ArgumentCaptor<ExecutionAttributes> attributesCaptor = ArgumentCaptor.forClass(ExecutionAttributes.class);
        doThrow(new RuntimeException("BOOM")).when(interceptor).beforeExecution(any(Context.BeforeExecution.class), attributesCaptor.capture());

        ProtocolRestJsonClient sync = ProtocolRestJsonClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar")))
                .region(Region.US_WEST_2)
                .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)
                        .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true))
                .build();

        thrown.expect(RuntimeException.class);
        try {
            sync.allTypes();
        } finally {
            ExecutionAttributes attributes = attributesCaptor.getValue();
            assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION)).isTrue();
            sync.close();
        }
    }

    @Test
    public void asyncClient_disableHostPrefixInjection_isPresent() {
        ExecutionInterceptor interceptor = mock(ExecutionInterceptor.class);
        ArgumentCaptor<ExecutionAttributes> attributesCaptor = ArgumentCaptor.forClass(ExecutionAttributes.class);
        doThrow(new RuntimeException("BOOM")).when(interceptor).beforeExecution(any(Context.BeforeExecution.class), attributesCaptor.capture());

        ProtocolRestJsonAsyncClient async = ProtocolRestJsonAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar")))
                .region(Region.US_WEST_2)
                .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)
                        .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true))
                .build();

        thrown.expect(CompletionException.class);
        try {
            async.allTypes().join();
        } finally {
            ExecutionAttributes attributes = attributesCaptor.getValue();
            assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION)).isTrue();
            async.close();
        }
    }
}

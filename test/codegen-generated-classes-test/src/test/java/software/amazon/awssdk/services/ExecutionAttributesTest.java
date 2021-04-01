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
import software.amazon.awssdk.core.interceptor.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;

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

    @Test
    public void asyncClient_clientOverrideExecutionAttribute() {
        ExecutionInterceptor interceptor = mock(ExecutionInterceptor.class);
        ArgumentCaptor<ExecutionAttributes> attributesCaptor = ArgumentCaptor.forClass(ExecutionAttributes.class);
        doThrow(new RuntimeException("BOOM")).when(interceptor).beforeExecution(any(Context.BeforeExecution.class), attributesCaptor.capture());
        ExecutionAttribute testAttribute = new ExecutionAttribute<>("TestAttribute");
        String testValue = "TestValue";

        ProtocolRestJsonAsyncClient async = ProtocolRestJsonAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar")))
                .region(Region.US_WEST_2)
                .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)
                        .putExecutionAttribute(testAttribute, testValue))
                .build();

        thrown.expect(CompletionException.class);
        try {
            async.allTypes().join();
        } finally {
            ExecutionAttributes attributes = attributesCaptor.getValue();
            assertThat(attributes.getAttribute(testAttribute)).isEqualTo(testValue);
            async.close();
        }
    }

    @Test
    public void asyncClient_requestOverrideExecutionAttribute() {
        ExecutionInterceptor interceptor = mock(ExecutionInterceptor.class);
        ArgumentCaptor<ExecutionAttributes> attributesCaptor = ArgumentCaptor.forClass(ExecutionAttributes.class);
        doThrow(new RuntimeException("BOOM")).when(interceptor).beforeExecution(any(Context.BeforeExecution.class), attributesCaptor.capture());
        ExecutionAttribute testAttribute = new ExecutionAttribute<>("TestAttribute");
        String testValue = "TestValue";

        ProtocolRestJsonAsyncClient async = ProtocolRestJsonAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar")))
                .region(Region.US_WEST_2)
                .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
                .build();

        AllTypesRequest request = AllTypesRequest.builder().overrideConfiguration(
                    c -> c.putExecutionAttribute(testAttribute, testValue))
                .build();

        thrown.expect(CompletionException.class);
        try {
            async.allTypes(request).join();
        } finally {
            ExecutionAttributes attributes = attributesCaptor.getValue();
            assertThat(attributes.getAttribute(testAttribute)).isEqualTo(testValue);
            async.close();
        }
    }

    @Test
    public void asyncClient_requestOverrideExecutionAttributesHavePrecedence() {
        ExecutionInterceptor interceptor = mock(ExecutionInterceptor.class);
        ArgumentCaptor<ExecutionAttributes> attributesCaptor = ArgumentCaptor.forClass(ExecutionAttributes.class);
        doThrow(new RuntimeException("BOOM")).when(interceptor).beforeExecution(any(Context.BeforeExecution.class), attributesCaptor.capture());
        ExecutionAttribute testAttribute = new ExecutionAttribute<>("TestAttribute");
        String testValue = "TestValue";

        ProtocolRestJsonAsyncClient async = ProtocolRestJsonAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar")))
                .region(Region.US_WEST_2)
                .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor).putExecutionAttribute(testAttribute, testValue))
                .build();

        String overwrittenValue = "TestValue2";
        AllTypesRequest request = AllTypesRequest.builder().overrideConfiguration(
                c -> c.putExecutionAttribute(testAttribute, overwrittenValue))
                .build();

        thrown.expect(CompletionException.class);
        try {
            async.allTypes(request).join();
        } finally {
            ExecutionAttributes attributes = attributesCaptor.getValue();
            assertThat(attributes.getAttribute(testAttribute)).isEqualTo(overwrittenValue);
            async.close();
        }
    }

    @Test
    public void testExecutionAttributesMerge() {
        ExecutionAttributes lowerPrecedence = new ExecutionAttributes();
        for (int i = 0; i < 3; i++) {
            lowerPrecedence.putAttribute(getAttribute(i), 1);
        }

        ExecutionAttributes higherPrecendence = new ExecutionAttributes();
        for (int i = 2; i < 4; i++) {
            higherPrecendence.putAttribute(getAttribute(i), 2);
        }

        ExecutionAttributes expectedAttributes = new ExecutionAttributes();
        for (int i = 0; i < 4; i++) {
            expectedAttributes.putAttribute(getAttribute(i),  i >= 2 ? 2 : 1);
        }

        ExecutionAttributes mergedAttributes = higherPrecendence.merge(lowerPrecedence);
        assertThat(mergedAttributes.getAttributes()).isEqualTo(expectedAttributes.getAttributes());
    }

    private ExecutionAttribute getAttribute(int i) {
        return new ExecutionAttribute<>("TestAttribute" + i);
    }
}

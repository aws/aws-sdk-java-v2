/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.internal.handlers;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ObjectMetadataInterceptorTest {
    private static final ObjectMetadataInterceptor INTERCEPTOR = new ObjectMetadataInterceptor();



    public static List<TestCase> testCases() {
        return asList(
            tc(asList("a", "b", "c"), asList("a", "b", "c")),
            tc(asList(" a ", "b", "c"), asList("a", "b", "c")),
            tc(asList("   a", "\tb", "\tc"), asList("a", "b", "c")),
            tc(asList("a\n", "\tb", "\tc\r\n"), asList("a", "b", "c"))

        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void modifyRequest_putObject_metadataKeysAreTrimmed(TestCase tc) {
        Map<String, String> metadata = tc.inputKeys.stream()
            .collect(Collectors.toMap(k -> k, k -> "value"));

        Context.ModifyHttpRequest ctx = mock(Context.ModifyHttpRequest.class);

        PutObjectRequest put = PutObjectRequest.builder()
            .metadata(metadata)
            .build();

        when(ctx.request()).thenReturn(put);

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.OPERATION_NAME, "PutObject");

        PutObjectRequest modified = (PutObjectRequest) INTERCEPTOR.modifyRequest(ctx, attrs);

        assertThat(modified.metadata().keySet()).containsExactlyElementsOf(tc.expectedKeys);
    }

    @Test
    public void modifyRequest_putObjectMetadataValueNull_shouldNotThrowException() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key", null);

        Context.ModifyHttpRequest ctx = mock(Context.ModifyHttpRequest.class);

        PutObjectRequest put = PutObjectRequest.builder()
                                               .metadata(metadata)
                                               .build();

        when(ctx.request()).thenReturn(put);

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.OPERATION_NAME, "PutObject");

        PutObjectRequest modified = (PutObjectRequest) INTERCEPTOR.modifyRequest(ctx, attrs);
        assertThat(modified.metadata().entrySet()).containsExactlyElementsOf(metadata.entrySet());
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void modifyRequest_creatMultipartUpload_metadataKeysAreTrimmed(TestCase tc) {
        Map<String, String> metadata = tc.inputKeys.stream()
                                                   .collect(Collectors.toMap(k -> k, k -> "value"));

        Context.ModifyHttpRequest ctx = mock(Context.ModifyHttpRequest.class);

        CreateMultipartUploadRequest mpu = CreateMultipartUploadRequest.builder()
                                                                       .metadata(metadata)
                                                                       .build();

        when(ctx.request()).thenReturn(mpu);

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.OPERATION_NAME, "CreateMultipartUpload");

        CreateMultipartUploadRequest modified = (CreateMultipartUploadRequest) INTERCEPTOR.modifyRequest(ctx, attrs);

        assertThat(modified.metadata().keySet()).containsExactlyElementsOf(tc.expectedKeys);
    }

    @Test
    public void modifyRequest_unknownOperation_ignores() {
        Context.ModifyHttpRequest ctx = mock(Context.ModifyHttpRequest.class);

        GetObjectRequest get = GetObjectRequest.builder().build();

        when(ctx.request()).thenReturn(get);

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.OPERATION_NAME, "GetObject");

        SdkRequest sdkRequest = INTERCEPTOR.modifyRequest(ctx, attrs);

        assertThat(sdkRequest).isEqualTo(get);
    }

    private static TestCase tc(List<String> input, List<String> expected) {
        return new TestCase(input, expected);
    }
    private static class TestCase {
        private List<String> inputKeys;
        private List<String> expectedKeys;

        public TestCase(List<String> inputKeys, List<String> expectedKeys) {
            this.inputKeys = inputKeys;
            this.expectedKeys = expectedKeys;
        }
    }
}

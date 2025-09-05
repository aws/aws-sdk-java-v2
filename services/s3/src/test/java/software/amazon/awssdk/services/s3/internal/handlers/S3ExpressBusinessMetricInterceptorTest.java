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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.s3.internal.s3express.S3ExpressUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

class S3ExpressBusinessMetricInterceptorTest {

    private S3ExpressBusinessMetricInterceptor interceptor;
    private ExecutionAttributes executionAttributes;
    private Context.ModifyRequest context;

    @BeforeEach
    void setUp() {
        interceptor = new S3ExpressBusinessMetricInterceptor();
        executionAttributes = new ExecutionAttributes();
        context = mock(Context.ModifyRequest.class);
    }

    @Test
    void s3ExpressOperation_shouldAddS3ExpressApiName() {
        GetObjectRequest s3Request = GetObjectRequest.builder().build();
        when(context.request()).thenReturn(s3Request);

        try (MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3Express(executionAttributes)).thenReturn(true);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes)).thenReturn(true);

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            RequestOverrideConfiguration requestOverrideConfiguration = modifiedRequest.overrideConfiguration().get();
            Predicate<ApiName> apiNamePredicate = a -> a.name().equals("sdk-metrics") &&
                                                       a.version().equals(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value());
            assertThat(requestOverrideConfiguration.apiNames().stream().anyMatch(apiNamePredicate)).isTrue();
        }
    }

    @Test
    void regularS3Operation_shouldNotAddS3ExpressApiName() {
        GetObjectRequest s3Request = GetObjectRequest.builder().build();
        when(context.request()).thenReturn(s3Request);

        try (MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3Express(executionAttributes)).thenReturn(false);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes)).thenReturn(false);

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            assertThat(modifiedRequest.overrideConfiguration()).isEmpty();
        }
    }

    @Test
    void s3ExpressEndpointWithoutS3ExpressAuth_shouldNotAddS3ExpressApiName() {
        GetObjectRequest s3Request = GetObjectRequest.builder().build();
        when(context.request()).thenReturn(s3Request);

        try (MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3Express(executionAttributes)).thenReturn(true);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes)).thenReturn(false);

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            assertThat(modifiedRequest.overrideConfiguration()).isEmpty();
        }
    }

    @Test
    void regularEndpointWithS3ExpressAuth_shouldNotAddS3ExpressApiName() {
        GetObjectRequest s3Request = GetObjectRequest.builder().build();
        when(context.request()).thenReturn(s3Request);

        try (MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3Express(executionAttributes)).thenReturn(false);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes)).thenReturn(true);

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            assertThat(modifiedRequest.overrideConfiguration()).isEmpty();
        }
    }

    @Test
    void nonS3Request_shouldNotBeModified() {
        SdkRequest nonS3Request = new SdkRequest() {
            @Override
            public List<SdkField<?>> sdkFields() {
                return null;
            }

            @Override
            public Optional<? extends RequestOverrideConfiguration> overrideConfiguration() {
                return Optional.empty();
            }

            @Override
            public Builder toBuilder() {
                return null;
            }
        };
        when(context.request()).thenReturn(nonS3Request);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        assertThat(modifiedRequest).isSameAs(nonS3Request);
    }

    @Test
    void s3ExpressOperationWithExistingOverrideConfig_shouldPreserveExistingConfig() {
        GetObjectRequest s3Request = GetObjectRequest.builder()
                                                     .overrideConfiguration(c -> c.putHeader("existing-header", "value"))
                                                     .build();
        when(context.request()).thenReturn(s3Request);

        try (MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3Express(executionAttributes)).thenReturn(true);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes)).thenReturn(true);

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            RequestOverrideConfiguration requestOverrideConfiguration = modifiedRequest.overrideConfiguration().get();
            
            // Check that S3 Express API name was added
            Predicate<ApiName> apiNamePredicate = a -> a.name().equals("sdk-metrics") &&
                                                       a.version().equals(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value());
            assertThat(requestOverrideConfiguration.apiNames().stream().anyMatch(apiNamePredicate)).isTrue();
            
            // Check that existing header was preserved
            assertThat(requestOverrideConfiguration.headers()).containsKey("existing-header");
            assertThat(requestOverrideConfiguration.headers().get("existing-header")).containsExactly("value");
        }
    }
}

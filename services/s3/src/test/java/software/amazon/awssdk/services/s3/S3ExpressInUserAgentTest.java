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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.s3.internal.BucketUtils;
import software.amazon.awssdk.services.s3.internal.handlers.S3ExpressBusinessMetricInterceptor;
import software.amazon.awssdk.services.s3.internal.s3express.S3ExpressUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Request;

public class S3ExpressInUserAgentTest {

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
    public void s3ExpressBucketWithS3ExpressAuth_shouldHaveS3ExpressApiName() {
        // Create S3 request with S3 Express bucket name
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("my-bucket--x-s3")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Mock BucketUtils and S3ExpressUtils
        try (MockedStatic<BucketUtils> mockedBucketUtils = mockStatic(BucketUtils.class);
             MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            
            mockedBucketUtils.when(() -> BucketUtils.isS3ExpressBucket("my-bucket--x-s3"))
                           .thenReturn(true);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes))
                               .thenReturn(true);

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            // Verify that the request has been modified with S3 Express API name
            assertThat(modifiedRequest).isInstanceOf(S3Request.class);
            S3Request modifiedS3Request = (S3Request) modifiedRequest;
            
            assertThat(modifiedS3Request.overrideConfiguration()).isPresent();
            AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
            
            // Check that the API name contains the S3 Express feature ID
            assertThat(overrideConfig.apiNames()).isNotEmpty();
            boolean hasS3ExpressApiName = overrideConfig.apiNames().stream()
                .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value()));
            assertThat(hasS3ExpressApiName).isTrue();
        }
    }

    @Test
    public void regularBucket_shouldNotHaveS3ExpressApiName() {
        // Create S3 request with regular bucket name
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("regular-bucket")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Mock BucketUtils and S3ExpressUtils
        try (MockedStatic<BucketUtils> mockedBucketUtils = mockStatic(BucketUtils.class);
             MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            
            mockedBucketUtils.when(() -> BucketUtils.isS3ExpressBucket("regular-bucket"))
                           .thenReturn(false);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes))
                               .thenReturn(false);

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            // Verify that the request has NOT been modified with S3 Express API name
            assertThat(modifiedRequest).isInstanceOf(S3Request.class);
            S3Request modifiedS3Request = (S3Request) modifiedRequest;
            
            // Either no override configuration or no S3 Express API name
            if (modifiedS3Request.overrideConfiguration().isPresent()) {
                AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
                boolean hasS3ExpressApiName = overrideConfig.apiNames().stream()
                    .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value()));
                assertThat(hasS3ExpressApiName).isFalse();
            }
        }
    }

    @Test
    public void s3ExpressBucketWithRegularAuth_shouldNotHaveS3ExpressApiName() {
        // Create S3 request with S3 Express bucket name but regular auth
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("my-bucket--x-s3")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Mock BucketUtils and S3ExpressUtils
        try (MockedStatic<BucketUtils> mockedBucketUtils = mockStatic(BucketUtils.class);
             MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            
            mockedBucketUtils.when(() -> BucketUtils.isS3ExpressBucket("my-bucket--x-s3"))
                           .thenReturn(true);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes))
                               .thenReturn(false); // Using regular SigV4, not S3 Express auth

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            // Verify that the request has NOT been modified with S3 Express API name
            assertThat(modifiedRequest).isInstanceOf(S3Request.class);
            S3Request modifiedS3Request = (S3Request) modifiedRequest;
            
            // Either no override configuration or no S3 Express API name
            if (modifiedS3Request.overrideConfiguration().isPresent()) {
                AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
                boolean hasS3ExpressApiName = overrideConfig.apiNames().stream()
                    .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value()));
                assertThat(hasS3ExpressApiName).isFalse();
            }
        }
    }

    @Test
    public void regularBucketWithS3ExpressAuth_shouldNotHaveS3ExpressApiName() {
        // Create S3 request with regular bucket name but S3 Express auth (edge case)
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("regular-bucket")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Mock BucketUtils and S3ExpressUtils
        try (MockedStatic<BucketUtils> mockedBucketUtils = mockStatic(BucketUtils.class);
             MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            
            mockedBucketUtils.when(() -> BucketUtils.isS3ExpressBucket("regular-bucket"))
                           .thenReturn(false);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes))
                               .thenReturn(true); // Using S3 Express auth but regular bucket

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            // Verify that the request has NOT been modified with S3 Express API name
            assertThat(modifiedRequest).isInstanceOf(S3Request.class);
            S3Request modifiedS3Request = (S3Request) modifiedRequest;
            
            // Either no override configuration or no S3 Express API name
            if (modifiedS3Request.overrideConfiguration().isPresent()) {
                AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
                boolean hasS3ExpressApiName = overrideConfig.apiNames().stream()
                    .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value()));
                assertThat(hasS3ExpressApiName).isFalse();
            }
        }
    }

    @Test
    public void nonS3Request_shouldNotBeModified() {
        // Create non-S3 request
        SdkRequest nonS3Request = mock(SdkRequest.class);
        when(context.request()).thenReturn(nonS3Request);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        // Verify that the request has NOT been modified (should return the same request)
        assertThat(modifiedRequest).isSameAs(nonS3Request);
    }

    @Test
    public void s3RequestWithNullBucket_shouldNotBeModified() {
        // Create S3 request with null bucket (edge case)
        S3Request s3Request = PutObjectRequest.builder()
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        // Verify that the request has NOT been modified (should return the same request)
        assertThat(modifiedRequest).isSameAs(s3Request);
    }

    @Test
    public void s3ExpressBucketWithExistingOverrideConfig_shouldPreserveExistingConfig() {
        // Create S3 request with existing override configuration
        ApiName existingApiName = ApiName.builder().name("existing").version("1.0").build();
        AwsRequestOverrideConfiguration existingConfig = AwsRequestOverrideConfiguration.builder()
                                                                                       .addApiName(existingApiName)
                                                                                       .build();
        
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("my-bucket--x-s3")
                                            .key("test-key")
                                            .overrideConfiguration(existingConfig)
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Mock BucketUtils and S3ExpressUtils
        try (MockedStatic<BucketUtils> mockedBucketUtils = mockStatic(BucketUtils.class);
             MockedStatic<S3ExpressUtils> mockedS3ExpressUtils = mockStatic(S3ExpressUtils.class)) {
            
            mockedBucketUtils.when(() -> BucketUtils.isS3ExpressBucket("my-bucket--x-s3"))
                           .thenReturn(true);
            mockedS3ExpressUtils.when(() -> S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes))
                               .thenReturn(true);

            SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

            // Verify that the request has been modified with S3 Express API name AND preserves existing config
            assertThat(modifiedRequest).isInstanceOf(S3Request.class);
            S3Request modifiedS3Request = (S3Request) modifiedRequest;
            
            assertThat(modifiedS3Request.overrideConfiguration()).isPresent();
            AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
            
            // Check that both existing and S3 Express API names are present
            assertThat(overrideConfig.apiNames()).hasSize(2);
            boolean hasExistingApiName = overrideConfig.apiNames().stream()
                .anyMatch(apiName -> apiName.name().equals("existing") && apiName.version().equals("1.0"));
            boolean hasS3ExpressApiName = overrideConfig.apiNames().stream()
                .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value()));
            
            assertThat(hasExistingApiName).isTrue();
            assertThat(hasS3ExpressApiName).isTrue();
        }
    }
}

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
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.s3.internal.handlers.S3AccessGrantsBusinessMetricInterceptor;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Request;

public class S3AccessGrantsInUserAgentTest {

    private S3AccessGrantsBusinessMetricInterceptor interceptor;
    private ExecutionAttributes executionAttributes;
    private Context.ModifyRequest context;

    @BeforeEach
    void setUp() {
        interceptor = new S3AccessGrantsBusinessMetricInterceptor();
        executionAttributes = new ExecutionAttributes();
        
        context = mock(Context.ModifyRequest.class);
    }

    @Test
    public void s3AccessGrantsCredentials_shouldHaveS3AccessGrantsApiName() {
        // Create S3 request
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("test-bucket")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Create credentials with S3 Access Grants provider name
        AwsCredentials s3AccessGrantsCredentials = createCredentialsWithProviderName("S3AccessGrantsIdentityProvider");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, s3AccessGrantsCredentials);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        // Verify that the request has been modified with S3 Access Grants API name
        assertThat(modifiedRequest).isInstanceOf(S3Request.class);
        S3Request modifiedS3Request = (S3Request) modifiedRequest;
        
        assertThat(modifiedS3Request.overrideConfiguration()).isPresent();
        AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
        
        // Check that the API name contains the S3 Access Grants feature ID
        assertThat(overrideConfig.apiNames()).isNotEmpty();
        boolean hasS3AccessGrantsApiName = overrideConfig.apiNames().stream()
            .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_ACCESS_GRANTS.value()));
        assertThat(hasS3AccessGrantsApiName).isTrue();
    }

    @Test
    public void regularCredentials_shouldNotHaveS3AccessGrantsApiName() {
        // Create S3 request
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("test-bucket")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Create credentials with regular provider name
        AwsCredentials regularCredentials = createCredentialsWithProviderName("DefaultCredentialsProvider");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, regularCredentials);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        // Verify that the request has NOT been modified with S3 Access Grants API name
        assertThat(modifiedRequest).isInstanceOf(S3Request.class);
        S3Request modifiedS3Request = (S3Request) modifiedRequest;
        
        // Either no override configuration or no S3 Access Grants API name
        if (modifiedS3Request.overrideConfiguration().isPresent()) {
            AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
            boolean hasS3AccessGrantsApiName = overrideConfig.apiNames().stream()
                .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_ACCESS_GRANTS.value()));
            assertThat(hasS3AccessGrantsApiName).isFalse();
        }
    }

    @Test
    public void accessGrantsProviderName_shouldHaveS3AccessGrantsApiName() {
        // Create S3 request
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("test-bucket")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Test with "AccessGrants" provider name variation
        AwsCredentials accessGrantsCredentials = createCredentialsWithProviderName("AccessGrants");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, accessGrantsCredentials);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        // Verify that the request has been modified with S3 Access Grants API name
        assertThat(modifiedRequest).isInstanceOf(S3Request.class);
        S3Request modifiedS3Request = (S3Request) modifiedRequest;
        
        assertThat(modifiedS3Request.overrideConfiguration()).isPresent();
        AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
        
        boolean hasS3AccessGrantsApiName = overrideConfig.apiNames().stream()
            .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_ACCESS_GRANTS.value()));
        assertThat(hasS3AccessGrantsApiName).isTrue();
    }

    @Test
    public void s3AccessGrantsProviderName_shouldHaveS3AccessGrantsApiName() {
        // Create S3 request
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("test-bucket")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Test with "S3AccessGrants" provider name variation
        AwsCredentials s3AccessGrantsCredentials = createCredentialsWithProviderName("S3AccessGrants");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, s3AccessGrantsCredentials);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        // Verify that the request has been modified with S3 Access Grants API name
        assertThat(modifiedRequest).isInstanceOf(S3Request.class);
        S3Request modifiedS3Request = (S3Request) modifiedRequest;
        
        assertThat(modifiedS3Request.overrideConfiguration()).isPresent();
        AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
        
        boolean hasS3AccessGrantsApiName = overrideConfig.apiNames().stream()
            .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_ACCESS_GRANTS.value()));
        assertThat(hasS3AccessGrantsApiName).isTrue();
    }

    @Test
    public void invalidProviderName_shouldNotHaveS3AccessGrantsApiName() {
        // Create S3 request
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("test-bucket")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // Test with provider name that should NOT trigger S3 Access Grants metric
        AwsCredentials invalidCredentials = createCredentialsWithProviderName("InstanceProfileCredentialsProvider");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, invalidCredentials);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        // Verify that the request has NOT been modified with S3 Access Grants API name
        assertThat(modifiedRequest).isInstanceOf(S3Request.class);
        S3Request modifiedS3Request = (S3Request) modifiedRequest;
        
        // Either no override configuration or no S3 Access Grants API name
        if (modifiedS3Request.overrideConfiguration().isPresent()) {
            AwsRequestOverrideConfiguration overrideConfig = modifiedS3Request.overrideConfiguration().get();
            boolean hasS3AccessGrantsApiName = overrideConfig.apiNames().stream()
                .anyMatch(apiName -> apiName.version().equals(BusinessMetricFeatureId.S3_ACCESS_GRANTS.value()));
            assertThat(hasS3AccessGrantsApiName).isFalse();
        }
    }

    @Test
    public void nonS3Request_shouldNotBeModified() {
        // Create non-S3 request
        SdkRequest nonS3Request = mock(SdkRequest.class);
        when(context.request()).thenReturn(nonS3Request);

        // Create credentials with S3 Access Grants provider name
        AwsCredentials s3AccessGrantsCredentials = createCredentialsWithProviderName("S3AccessGrantsIdentityProvider");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, s3AccessGrantsCredentials);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        // Verify that the request has NOT been modified (should return the same request)
        assertThat(modifiedRequest).isSameAs(nonS3Request);
    }

    @Test
    public void noCredentials_shouldNotModifyRequest() {
        // Create S3 request
        S3Request s3Request = PutObjectRequest.builder()
                                            .bucket("test-bucket")
                                            .key("test-key")
                                            .build();
        when(context.request()).thenReturn(s3Request);

        // No credentials set
        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        // Verify that the request has NOT been modified (should return the same request)
        assertThat(modifiedRequest).isSameAs(s3Request);
    }

    private AwsCredentials createCredentialsWithProviderName(String providerName) {
        AwsCredentials baseCredentials = AwsBasicCredentials.create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        return new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return baseCredentials.accessKeyId();
            }

            @Override
            public String secretAccessKey() {
                return baseCredentials.secretAccessKey();
            }

            @Override
            public Optional<String> providerName() {
                return Optional.of(providerName);
            }
        };
    }
}

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

package software.amazon.awssdk.auth.credentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.internal.ImdsCredentialsBusinessMetricInterceptor;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;

public class ImdsCredentialsInUserAgentTest {

    private ImdsCredentialsBusinessMetricInterceptor interceptor;
    private ExecutionAttributes executionAttributes;
    private BusinessMetricCollection businessMetrics;
    private Context.ModifyRequest context;

    @BeforeEach
    void setUp() {
        interceptor = new ImdsCredentialsBusinessMetricInterceptor();
        executionAttributes = new ExecutionAttributes();
        businessMetrics = new BusinessMetricCollection();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS, businessMetrics);

        context = mock(Context.ModifyRequest.class);
        SdkRequest request = mock(SdkRequest.class);
        when(context.request()).thenReturn(request);
    }

    @Test
    public void imdsCredentials_shouldHaveImdsCredentialsBusinessMetric() {
        // Create credentials with IMDS provider name
        AwsCredentials imdsCredentials = createCredentialsWithProviderName("InstanceProfileCredentialsProvider");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, imdsCredentials);

        interceptor.modifyRequest(context, executionAttributes);

        // Verify that the CREDENTIALS_IMDS metric is added to business metrics
        assertThat(businessMetrics.recordedMetrics())
            .contains(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
        
        // Verify that the metric appears in the user agent string
        String userAgentString = businessMetrics.asBoundedString();
        assertThat(userAgentString).contains(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
    }

    @Test
    public void containerCredentials_shouldNotHaveImdsCredentialsBusinessMetric() {
        // Test with "ContainerCredentialsProvider" provider name - should not be considered IMDS
        AwsCredentials containerCredentials = createCredentialsWithProviderName("ContainerCredentialsProvider");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, containerCredentials);

        interceptor.modifyRequest(context, executionAttributes);

        assertThat(businessMetrics.recordedMetrics())
            .doesNotContain(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
    }

    @Test
    public void credentialsWithoutProviderName_shouldNotHaveImdsCredentialsBusinessMetric() {
        // Test with credentials that don't have a provider name
        AwsCredentials credentialsWithoutProviderName = AwsBasicCredentials.create("accessKey", "secretKey");
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, credentialsWithoutProviderName);

        interceptor.modifyRequest(context, executionAttributes);

        assertThat(businessMetrics.recordedMetrics())
            .doesNotContain(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
    }

    private AwsCredentials createCredentialsWithProviderName(String providerName) {
        AwsCredentials baseCredentials = AwsBasicCredentials.create("test-access-key", "test-secret-key");
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

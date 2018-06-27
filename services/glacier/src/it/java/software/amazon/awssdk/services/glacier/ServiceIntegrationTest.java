/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.glacier;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.glacier.model.ListVaultsRequest;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class ServiceIntegrationTest extends AwsIntegrationTestBase {

    private GlacierClient client;

    private CapturingExecutionInterceptor capturingExecutionInterceptor = new CapturingExecutionInterceptor();

    @Before
    public void setup() {
        client = GlacierClient.builder()
                              .credentialsProvider(getCredentialsProvider())
                              .overrideConfiguration(ClientOverrideConfiguration
                                                             .builder()
                                                             .addExecutionInterceptor(capturingExecutionInterceptor)
                                                             .build())
                              .build();
    }

    /**
     * API version is a required parameter inserted by the
     * {@link software.amazon.awssdk.services.glacier.internal.GlacierExecutionInterceptor}
     */
    @Test
    public void listVaults_SendsApiVersion() {
        client.listVaults(ListVaultsRequest.builder().build());
        assertThat(capturingExecutionInterceptor.beforeTransmission)
                .is(new Condition<>(r -> r.firstMatchingHeader("x-amz-glacier-version")
                                          .orElseThrow(() -> new AssertionError("x-amz-glacier-version header not found"))
                                          .equals("2012-06-01"),
                                    "Glacier API version is present in header"));
    }

    public static class CapturingExecutionInterceptor implements ExecutionInterceptor {

        private SdkHttpFullRequest beforeTransmission;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.beforeTransmission = context.httpRequest();
        }

    }
}

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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.interceptor.TraceIdExecutionInterceptor;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Verifies that the {@link TraceIdExecutionInterceptor} is actually wired up for AWS services.
 */
public class TraceIdTest {
    @Test
    public void traceIdInterceptorIsEnabled() {
        EnvironmentVariableHelper.run(env -> {
            env.set("AWS_LAMBDA_FUNCTION_NAME", "foo");
            env.set("_X_AMZN_TRACE_ID", "bar");

            try (MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
                 ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                                       .region(Region.US_WEST_2)
                                                                       .credentialsProvider(AnonymousCredentialsProvider.create())
                                                                       .httpClient(mockHttpClient)
                                                                       .build()) {
                mockHttpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                                   .response(SdkHttpResponse.builder()
                                                                                            .statusCode(200)
                                                                                            .build())
                                                                   .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                                                   .build());
                client.allTypes();
                assertThat(mockHttpClient.getLastRequest().firstMatchingHeader("X-Amzn-Trace-Id")).hasValue("bar");
            }
        });
    }
}

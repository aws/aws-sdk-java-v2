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

package software.amazon.awssdk.services.s3.functionaltests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class RetriesOn200Test {
    public static final String ERROR_CODE = "InternalError";
    public static final String ERROR_MESSAGE = "We encountered an internal error. Please try again.";
    public static final String ERROR_BODY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                            + "<Error>\n"
                                            + "  <Code>" + ERROR_CODE + "</Code>\n"
                                            + "  <Message>" + ERROR_MESSAGE + "</Message>\n"
                                            + "</Error>";

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    @Test
    public void copyObjectRetriesOn200InternalErrorFailures() {
        AttemptCountingInterceptor countingInterceptor = new AttemptCountingInterceptor();
        S3Client client = S3Client.builder()
                                  .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                  .region(Region.US_WEST_2)
                                  .credentialsProvider(AnonymousCredentialsProvider.create())
                                  .overrideConfiguration(c -> c.retryPolicy(RetryMode.STANDARD)
                                                               .addExecutionInterceptor(countingInterceptor))
                                  .serviceConfiguration(c -> c.pathStyleAccessEnabled(true))
                                  .build();


        stubFor(put(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withBody(ERROR_BODY)));

        assertThatThrownBy(() -> client.copyObject(r -> r.sourceBucket("foo").sourceKey("foo")
                                                         .destinationBucket("bar").destinationKey("bar")))
            .isInstanceOfSatisfying(S3Exception.class, e -> {
                assertThat(e.statusCode()).isEqualTo(200);
                assertThat(e.awsErrorDetails().errorCode()).isEqualTo(ERROR_CODE);
                assertThat(e.awsErrorDetails().errorMessage()).isEqualTo(ERROR_MESSAGE);
            });
        assertThat(countingInterceptor.attemptCount).isEqualTo(RetryPolicy.forRetryMode(RetryMode.STANDARD).numRetries() + 1);
    }

    private static final class AttemptCountingInterceptor implements ExecutionInterceptor {
        private long attemptCount = 0;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            ++attemptCount;
        }
    }
}

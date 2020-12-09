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

package software.amazon.awssdk.services.s3control.internal.functionaltests.signing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;


public class UrlEncodingTest {
    private static final URI HTTP_LOCALHOST_URI = URI.create("http://localhost:8080/");
    private static final String EXPECTED_URL = "/v20180820/jobs/id";

    private S3ControlClient s3Control;
    private ExecutionAttributeInterceptor interceptor;

    @Rule
    public WireMockRule mockServer = new WireMockRule();

    protected S3ControlClient buildClient() {
        this.interceptor = new ExecutionAttributeInterceptor(HTTP_LOCALHOST_URI);

        return S3ControlClient.builder()
                              .credentialsProvider(() -> AwsBasicCredentials.create("test", "test"))
                              .region(Region.US_WEST_2)
                              .overrideConfiguration(o -> o.addExecutionInterceptor(this.interceptor))
                              .build();
    }

    @Before
    public void methodSetUp() {
        s3Control = buildClient();
    }

    @Test
    public void any_request_should_set_double_url_encode_to_false() {
        stubFor(get(urlMatching(EXPECTED_URL)).willReturn(aResponse().withBody("<xml></xml>").withStatus(200)));

        s3Control.describeJob(b -> b.accountId("123456789012").jobId("id"));

        assertThat(interceptor.signerDoubleUrlEncode()).isNotNull();
        assertThat(interceptor.signerDoubleUrlEncode()).isFalse();
    }

    /**
     * In addition to checking the signing attribute, the interceptor sets the endpoint since
     * S3 control prepends the account id to the host name and wiremock won't intercept the request
     */
    private static class ExecutionAttributeInterceptor implements ExecutionInterceptor {
        private final URI rerouteEndpoint;
        private Boolean signerDoubleUrlEncode;

        ExecutionAttributeInterceptor(URI rerouteEndpoint) {
            this.rerouteEndpoint = rerouteEndpoint;
        }

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            signerDoubleUrlEncode = executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE);
        }

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            SdkHttpRequest request = context.httpRequest();
            return request.toBuilder().uri(rerouteEndpoint).build();
        }

        public Boolean signerDoubleUrlEncode() {
            return signerDoubleUrlEncode;
        }
    }
}

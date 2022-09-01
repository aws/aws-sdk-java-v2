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

package software.amazon.awssdk.auth.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.URI;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.signer.SdkTokenExecutionAttribute;
import software.amazon.awssdk.auth.token.TestBearerToken;
import software.amazon.awssdk.auth.signer.params.TokenSignerParams;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

class BearerTokenSignerTest {

    private static final String BEARER_AUTH_MARKER = "Bearer ";

    @Test
    public void whenTokenExists_requestIsSignedCorrectly() {
        String tokenValue = "mF_9.B5f-4.1JqM";

        BearerTokenSigner tokenSigner = BearerTokenSigner.create();
        SdkHttpFullRequest signedRequest = tokenSigner.sign(generateBasicRequest(),
                                                            executionAttributes(TestBearerToken.create(tokenValue)));


        String expectedHeader = createExpectedHeader(tokenValue);
        assertThat(signedRequest.firstMatchingHeader("Authorization")).hasValue(expectedHeader);
    }

    @Test
    public void whenTokenIsMissing_exceptionIsThrown() {
        BearerTokenSigner tokenSigner = BearerTokenSigner.create();
        assertThatThrownBy(() -> tokenSigner.sign(generateBasicRequest(), executionAttributes(null)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("token");
    }

    @Test
    public void usingParamMethod_worksCorrectly() {
        String tokenValue = "mF_9.B5f-4.1JqM";

        BearerTokenSigner tokenSigner = BearerTokenSigner.create();
        SdkHttpFullRequest signedRequest = tokenSigner.sign(generateBasicRequest(),
                                                            TokenSignerParams.builder()
                                                                             .token(TestBearerToken.create(tokenValue))
                                                                             .build());

        String expectedHeader = createExpectedHeader(tokenValue);
        assertThat(signedRequest.firstMatchingHeader("Authorization")).hasValue(expectedHeader);
    }

    private static String createExpectedHeader(String token) {
        return BEARER_AUTH_MARKER + token;
    }

    private static ExecutionAttributes executionAttributes(TestBearerToken token) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkTokenExecutionAttribute.SDK_TOKEN, token);
        return executionAttributes;
    }

    private static SdkHttpFullRequest generateBasicRequest() {
        return SdkHttpFullRequest.builder()
                                 .contentStreamProvider(() -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                 .putHeader("x-amz-archive-description", "test  test")
                                 .encodedPath("/")
                                 .uri(URI.create("http://demo.us-east-1.amazonaws.com"))
                                 .build();
    }
}

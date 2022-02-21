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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.auth.signer.params.SignerChecksumParams;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Aws4UnsignedPayloadSignerTest {
    final static SignerChecksumParams CRC32_HEADER_SIGNER_PARAMS =
        SignerChecksumParams.builder()
                            .algorithm(Algorithm.CRC32)
                            .checksumHeaderName("x-amzn-header-crc")
                            .isStreamingRequest(false)
                            .build();
    AwsBasicCredentials credentials;
    Aws4SignerParams signerParams;
    @Mock
    private Clock signingOverrideClock;

    @Before
    public void setupCase() {
        mockClock();
        credentials = AwsBasicCredentials.create("access", "secret");
        signerParams = Aws4SignerParams.builder()
                                       .awsCredentials(credentials)
                                       .signingName("demo")
                                       .checksumParams(CRC32_HEADER_SIGNER_PARAMS)
                                       .signingClockOverride(signingOverrideClock)
                                       .signingRegion(Region.of("us-east-1"))
                                       .build();
    }

    @Test
    public void testAws4UnsignedPayloadSignerUsingHttpWherePayloadIsSigned() {
        final Aws4UnsignedPayloadSigner signer = Aws4UnsignedPayloadSigner.create();
        SdkHttpFullRequest.Builder request = getHttpRequestBuilder("abc", "http");
        SdkHttpFullRequest signed = signer.sign(request.build(), signerParams);
        assertThat(signed.firstMatchingHeader("x-amzn-header-crc")).hasValue("NSRBwg==");
    }

    @Test
    public void testAws4UnsignedPayloadSignerWithHttpsRequest() {
        final Aws4UnsignedPayloadSigner signer = Aws4UnsignedPayloadSigner.create();
        SdkHttpFullRequest.Builder request = getHttpRequestBuilder("abc", "https");
        SdkHttpFullRequest signed = signer.sign(request.build(), signerParams);
        assertThat(signed.firstMatchingHeader("x-amzn-header-crc")).isNotPresent();
    }

    private void mockClock() {
        Calendar c = new GregorianCalendar();
        c.set(1981, 1, 16, 6, 30, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        when(signingOverrideClock.millis()).thenReturn(c.getTimeInMillis());
    }

    private SdkHttpFullRequest.Builder getHttpRequestBuilder(String testString, String protocol) {
        SdkHttpFullRequest.Builder request = SdkHttpFullRequest.builder()
                                                               .contentStreamProvider(() -> new ByteArrayInputStream(testString.getBytes()))
                                                               .method(SdkHttpMethod.POST)
                                                               .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                               .putHeader("x-amz-archive-description", "test  test")
                                                               .encodedPath("/")
                                                               .protocol(protocol)
                                                               .uri(URI.create(protocol + "://demo.us-east-1.amazonaws.com"));
        return request;
    }
}

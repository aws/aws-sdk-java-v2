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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.URI;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;

@WireMockTest
public class ExpiresHeaderDataTypeErrorTest {

    S3Client s3Client;
    private final String TEST_DATE = "2034-02-01T00:00:00Z";

    @BeforeEach
    public void initWireMock(WireMockRuntimeInfo wm) {
        s3Client = S3Client.builder().endpointOverride(URI.create(wm.getHttpBaseUrl()))
                           .credentialsProvider(AnonymousCredentialsProvider.create())
                           .build();
    }

    @Test
    public void headObjectRequestWithInvalidDate_doesNotThrowException() throws IOException {

        stubFor(any(anyUrl())
                    .willReturn(aResponse()
                                    .withHeader("Expires", TEST_DATE)
                                    .withBody("Hello world!")));

        Assertions.assertThatCode(() -> s3Client.headObject(r -> {
                      r.bucket("s3_expires_test_dummy_bucket")
                       .key("s3_expires_test_dummy_key");
                  }))
                  .doesNotThrowAnyException();

        assertTrue(s3Client.headObject(r -> {r.bucket("s3_expires_test_dummy_bucket")
                                              .key("s3_expires_test_dummy_key");}).expires() == null);

        assertEquals(s3Client.headObject(r -> {r.bucket("s3_expires_test_dummy_bucket")
                                                .key("s3_expires_test_dummy_key");}).expiresString(), TEST_DATE);

    }

    @Test
    public void getObjectRequestWithInvalidDate_doesNotThrowException() throws IOException {

        stubFor(any(anyUrl())
                    .willReturn(aResponse()
                                    .withHeader("Expires", TEST_DATE)
                                    .withBody("Hello world!")));

        Assertions.assertThatCode(() -> s3Client.headObject(r -> {
                      r.bucket("s3_expires_test_dummy_bucket")
                       .key("s3_expires_test_dummy_key");
                  }))
                  .doesNotThrowAnyException();

        assertTrue(s3Client.getObject(r -> {r.bucket("s3_expires_test_dummy_bucket")
                                              .key("s3_expires_test_dummy_key");}).response().expires() == null);

        assertEquals(s3Client.getObject(r -> {r.bucket("s3_expires_test_dummy_bucket")
                                                .key("s3_expires_test_dummy_key");}).response().expiresString(), TEST_DATE);

    }
}

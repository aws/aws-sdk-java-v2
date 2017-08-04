/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

public class QueryStringSignerTest {

    private static final QueryStringSigner signer = new QueryStringSigner();
    private static final AwsCredentials credentials = new AwsCredentials("123456789", "123456789");

    private static final String EXPECTED_SIGNATURE = "VjYMhf9TWp08zAxXbKDAvUhW9GjJ56QjAuSj3LBsfjM=";

    static {
        Calendar c = new GregorianCalendar();
        c.clear();
        c.set(1981, 1, 16, 6, 30, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        signer.overrideDate(c.getTime());
    }

    @Test
    public void testRequestResourcePath() throws Exception {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .httpMethod(SdkHttpMethod.POST)
                                                       .endpoint(URI.create("http://foo.amazon.com"))
                                                       .queryParameter("foo", "bar")
                                                       .resourcePath("foo/bar")
                                                       .build();

        request = SignerTestUtils.signRequest(signer, request, credentials);

        assertSignature(EXPECTED_SIGNATURE, request.getParameters());
    }

    @Test
    public void testRequestAndEndpointResourcePath() throws Exception {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .httpMethod(SdkHttpMethod.POST)
                                                       .endpoint(URI.create("http://foo.amazon.com/foo"))
                                                       .queryParameter("foo", "bar")
                                                       .resourcePath("/bar")
                                                       .build();

        request = SignerTestUtils.signRequest(signer, request, credentials);

        assertSignature(EXPECTED_SIGNATURE, request.getParameters());
    }

    @Test
    public void testRequestAndEndpointResourcePathNoSlash() throws Exception {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .httpMethod(SdkHttpMethod.POST)
                                                       .endpoint(URI.create("http://foo.amazon.com/foo"))
                                                       .queryParameter("foo", "bar")
                                                       .resourcePath("bar")
                                                       .build();

        request = SignerTestUtils.signRequest(signer, request, credentials);

        assertSignature(EXPECTED_SIGNATURE, request.getParameters());
    }

    @Test
    public void testAnonymous() throws Exception {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .httpMethod(SdkHttpMethod.POST)
                                                       .endpoint(URI.create("http://foo.amazon.com"))
                                                       .queryParameter("foo", "bar")
                                                       .resourcePath("bar")
                                                       .build();

        request = SignerTestUtils.signRequest(signer, request, new AnonymousCredentialsProvider().getCredentials());

        assertNull(request.getParameters().get("Signature"));
    }

    /**
     * Asserts the given signature to the signature generated as part of the
     * signing the request.
     */
    private void assertSignature(String expected,
                                 Map<String, List<String>> requestParams) {
        List<String> signature = requestParams.get("Signature");
        assertNotNull(signature);
        assertEquals(1, signature.size());
        assertEquals(expected, signature.iterator().next());
    }

}

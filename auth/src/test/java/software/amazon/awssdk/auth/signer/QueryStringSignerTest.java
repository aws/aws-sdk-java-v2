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

package software.amazon.awssdk.auth.signer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsExecutionAttributes;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

public class QueryStringSignerTest {

    private static final AwsCredentials credentials = AwsCredentials.create("123456789", "123456789");
    private static final String EXPECTED_SIGNATURE = "VjYMhf9TWp08zAxXbKDAvUhW9GjJ56QjAuSj3LBsfjM=";

    private static QueryStringSigner signer;

    @BeforeClass
    public static void setup() {
        Calendar c = new GregorianCalendar();
        c.clear();
        c.set(1981, 1, 16, 6, 30, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        signer = QueryStringSigner.builder().overriddenDate(c.getTime()).build();
    }

    @Test
    public void testNonAnonymousRequest() throws Exception {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .protocol("http")
                                                       .host("foo.amazon.com")
                                                       .encodedPath("foo/bar")
                                                       .method(SdkHttpMethod.POST)
                                                       .rawQueryParameter("foo", "bar")
                                                       .build();


        request = signer.sign(request, constructAttributes(credentials));

        assertSignature(EXPECTED_SIGNATURE, request.rawQueryParameters());
    }

    @Test
    public void testAnonymous() throws Exception {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .protocol("http")
                                                       .host("foo.amazon.com")
                                                       .encodedPath("bar")
                                                       .method(SdkHttpMethod.POST)
                                                       .rawQueryParameter("foo", "bar")
                                                       .build();

        request = signer.sign(request, constructAttributes(AnonymousCredentialsProvider.create().getCredentials()));

        assertNull(request.rawQueryParameters().get("Signature"));
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

    private ExecutionAttributes constructAttributes(AwsCredentials awsCredentials) {
        return new ExecutionAttributes().putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS, awsCredentials);
    }
}

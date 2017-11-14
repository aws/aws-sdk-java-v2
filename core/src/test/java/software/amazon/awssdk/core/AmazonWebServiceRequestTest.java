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

package software.amazon.awssdk.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.RequestClientOptions.Marker;
import software.amazon.awssdk.core.auth.AwsCredentials;
import utils.model.EmptyAmazonWebServiceRequest;

public class AmazonWebServiceRequestTest {

    public static void verifyBaseBeforeCopy(final AmazonWebServiceRequest to) {
        assertNull(to.getCustomRequestHeaders());
        assertNull(to.getCustomQueryParameters());

        assertTrue(RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE == to
                .getReadLimit());
        RequestClientOptions toOptions = to.getRequestClientOptions();
        assertNull(toOptions.getClientMarker(Marker.USER_AGENT));
        assertTrue(RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE == toOptions
                .getReadLimit());
    }

    private static void verifyBaseAfterCopy(final AwsCredentials credentials,
                                            final AmazonWebServiceRequest from, final AmazonWebServiceRequest to) {
        Map<String, String> headers = to.getCustomRequestHeaders();
        assertTrue(2 == headers.size());
        assertEquals("v1", headers.get("k1"));
        assertEquals("v2", headers.get("k2"));
        Map<String, List<String>> parmas = to.getCustomQueryParameters();
        assertTrue(2 == parmas.size());
        assertEquals(Arrays.asList("v1"), parmas.get("k1"));
        assertEquals(Arrays.asList("v2a", "v2b"), parmas.get("k2"));

        assertTrue(1234 == to.getReadLimit());
        RequestClientOptions toOptions = to.getRequestClientOptions();
        assertEquals(
                from.getRequestClientOptions().getClientMarker(
                    Marker.USER_AGENT),
                toOptions.getClientMarker(Marker.USER_AGENT));
        assertTrue(1234 == toOptions.getReadLimit());
    }

    @Test
    public void testClone() {
        AmazonWebServiceRequest root = new AmazonWebServiceRequest() {
        };
        assertNull(root.getCloneSource());
        assertNull(root.getCloneRoot());

        AmazonWebServiceRequest clone = root.clone();
        assertEquals(root, clone.getCloneSource());
        assertEquals(root, clone.getCloneRoot());

        AmazonWebServiceRequest clone2 = clone.clone();
        assertEquals(clone, clone2.getCloneSource());
        assertEquals(root, clone2.getCloneRoot());
    }

    @Test
    public void copyBaseTo() {
        final AwsCredentials credentials = AwsCredentials.create("accesskey",
                                                                 "accessid");

        final AmazonWebServiceRequest from = new AmazonWebServiceRequest() {
        };
        from.setRequestCredentials(credentials);
        from.putCustomRequestHeader("k1", "v1");
        from.putCustomRequestHeader("k2", "v2");
        from.putCustomQueryParameter("k1", "v1");
        from.putCustomQueryParameter("k2", "v2a");
        from.putCustomQueryParameter("k2", "v2b");
        from.getRequestClientOptions().setReadLimit(1234);

        final AmazonWebServiceRequest to = new AmazonWebServiceRequest() {
        };

        // Before copy
        RequestClientOptions toOptions;
        verifyBaseBeforeCopy(to);

        // After copy
        from.copyBaseTo(to);
        verifyBaseAfterCopy(credentials, from, to);
    }

    @Test
    public void nullCredentialsSet_ReturnsNullProvider() {
        AmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        request.setRequestCredentials(null);
        assertNull(request.getRequestCredentialsProvider());
    }
}

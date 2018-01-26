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

package software.amazon.awssdk.core.runtime.endpoint;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.core.regions.Region;

public class DefaultServiceEndpointBuilderTest {

    @Test
    public void getServiceEndpoint_S3StandardRegion_HttpsProtocol() throws Exception {
        DefaultServiceEndpointBuilder endpointBuilder = new DefaultServiceEndpointBuilder("s3", "https")
                .withRegion(Region.US_EAST_1);
        assertEquals("https://s3.amazonaws.com", endpointBuilder.getServiceEndpoint().toString());
    }

    @Test
    public void getServiceEndpoint_S3StandardRegion_HttpProtocol() throws Exception {
        DefaultServiceEndpointBuilder endpointBuilder = new DefaultServiceEndpointBuilder("s3", "http")
                .withRegion(Region.US_EAST_1);
        assertEquals("http://s3.amazonaws.com", endpointBuilder.getServiceEndpoint().toString());
    }

    @Test
    public void getServiceEndpoint_S3NonStandardRegion_HttpProtocol() throws Exception {
        DefaultServiceEndpointBuilder endpointBuilder = new DefaultServiceEndpointBuilder("s3", "http")
                .withRegion(Region.EU_CENTRAL_1);
        assertEquals("http://s3.eu-central-1.amazonaws.com", endpointBuilder.getServiceEndpoint().toString());
    }
}

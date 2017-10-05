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

package software.amazon.awssdk.http;

import org.junit.After;
import org.junit.Before;
import software.amazon.awssdk.http.server.MockServer;
import software.amazon.awssdk.internal.http.request.EmptyHttpRequest;

public abstract class MockServerTestBase {

    protected MockServer server;

    @Before
    public void setupBaseFixture() {
        server = buildMockServer();
        server.startServer();
    }

    @After
    public void tearDownBaseFixture() {
        server.stopServer();
    }

    protected EmptyHttpRequest newGetRequest() {
        return new EmptyHttpRequest(server.getEndpoint(), HttpMethodName.GET);
    }

    /**
     * Implemented by test subclasses to build the correct type of {@link MockServer}
     */
    protected abstract MockServer buildMockServer();
}

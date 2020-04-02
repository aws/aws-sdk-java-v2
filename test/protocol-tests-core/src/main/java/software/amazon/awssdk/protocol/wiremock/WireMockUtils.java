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

package software.amazon.awssdk.protocol.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;

/**
 * Utils to start the WireMock server and retrieve the chosen port.
 */
public final class WireMockUtils {

    // Use 0 to dynamically assign an available port.
    private static final WireMockServer WIRE_MOCK = new WireMockServer(wireMockConfig().port(0));

    private WireMockUtils() {
    }

    public static void startWireMockServer() {
        WIRE_MOCK.start();
        WireMock.configureFor(WIRE_MOCK.port());
    }

    /**
     * @return The port that was chosen by the WireMock server.
     */
    public static int port() {
        return WIRE_MOCK.port();
    }

    /**
     * @return All LoggedRequests that wire mock captured.
     */
    public static List<LoggedRequest> findAllLoggedRequests() {
        List<LoggedRequest> requests = findAll(
                new RequestPatternBuilder(RequestMethod.ANY, urlMatching(".*")));
        return requests;
    }

    public static void verifyRequestCount(int expectedCount, WireMockRule wireMock) {
        wireMock.verify(expectedCount, anyRequestedFor(anyUrl()));
    }
}

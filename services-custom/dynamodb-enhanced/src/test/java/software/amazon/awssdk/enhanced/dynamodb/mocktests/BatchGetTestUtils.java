/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.mocktests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import com.github.tomakehurst.wiremock.stubbing.Scenario;

public class BatchGetTestUtils {

    private BatchGetTestUtils() {
    }

    static final String RESPONSE_WITHOUT_UNPROCESSED_KEYS = "{\"Responses\":{\"table\":[{\"id\":{\"N\":\"1\"},"
                                                            + "\"value\":{\"N\":\"2\"}},{\"id\":{\"N\":\"2\"},"
                                                            + "\"value\":{\"N\":\"0\"}},{\"id\":{\"N\":\"0\"},"
                                                            + "\"value\":{\"N\":\"0\"}}]},\"UnprocessedKeys\":{}}";

    static final String RESPONSE_WITH_UNPROCESSED_KEYS = "{\"Responses\":{\"table\":[{\"id\":{\"N\":\"1\"},"
                                                         + "\"value\":{\"N\":\"2\"}},{\"id\":{\"N\":\"0\"},"
                                                         + "\"value\":{\"N\":\"0\"}}]},\"UnprocessedKeys\":{\"table"
                                                         + "\": {\"Keys\": [{\"id\": {\"N\": \"2\"}}]}}}";

    static final String RESPONSE_WITH_UNPROCESSED_KEYS_PROCESSED = "{\"Responses\":{\"table\":[{\"id\":{\"N\":\"2\"},"
                                                                   + "\"value\":{\"N\":\"0\"}}]},\"UnprocessedKeys\":{}}";

    static void stubSuccessfulResponse() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody(RESPONSE_WITHOUT_UNPROCESSED_KEYS)));
    }

    static void stubResponseWithUnprocessedKeys() {
        stubFor(post(anyUrl())
                    .inScenario("unprocessed keys")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse().withStatus(200)
                                           .withBody(RESPONSE_WITH_UNPROCESSED_KEYS)));

        stubFor(post(anyUrl())
                    .inScenario("unprocessed keys")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse().withStatus(200)
                                           .withBody(RESPONSE_WITH_UNPROCESSED_KEYS_PROCESSED)));
    }


    static final class Record {
        private int id;

        Integer getId() {
            return id;
        }

        Record setId(Integer id) {
            this.id = id;
            return this;
        }
    }

}

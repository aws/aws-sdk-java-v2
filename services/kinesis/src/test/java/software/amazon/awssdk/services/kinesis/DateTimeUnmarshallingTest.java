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

package software.amazon.awssdk.services.kinesis;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.SdkSystemSetting.CBOR_ENABLED;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.Record;

@RunWith(MockitoJUnitRunner.class)
public class DateTimeUnmarshallingTest {

    private KinesisClient client;

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Before
    public void setup() {
        System.setProperty(CBOR_ENABLED.property(), "false");
        client = KinesisClient.builder()
                              .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                              .credentialsProvider(() -> AwsBasicCredentials.create("test", "test"))
                              .region(Region.US_EAST_1)
                              .build();
    }

    @Test
    public void cborDisabled_dateUnmarshalling_shouldSucceed() {
        String content = content();
        int length = content.getBytes().length;
        Instant instant =  Instant.ofEpochMilli(1548118964772L);

        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("Content-Length", String.valueOf(length))
                                           .withBody(content)));


        List<Record> records = client.getRecords(b -> b.shardIterator("test")).records();

        assertThat(records).isNotEmpty();
        assertThat(records.get(0).approximateArrivalTimestamp()).isEqualTo(instant);
    }

    private String content() {
         return "{\n"
                + "  \"MillisBehindLatest\": 0,\n"
                + "  \"NextShardIterator\": \"test\",\n"
                + "  \"Records\": [{\n"
                + "    \"ApproximateArrivalTimestamp\": 1.548118964772E9,\n"
                + "    \"Data\": \"U2VlIE5vIEV2aWw=\",\n"
                + "    \"PartitionKey\": \"foobar\",\n"
                + "    \"SequenceNumber\": \"12345678\"\n"
                + "  }]\n"
                + "}";
    }
}

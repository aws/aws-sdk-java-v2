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

package software.amazon.awssdk.services.iot;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.iotdataplane.IoTDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.DeleteThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.DeleteThingShadowResponse;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowResponse;
import software.amazon.awssdk.services.iotdataplane.model.InvalidRequestException;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;
import software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowResponse;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;
import software.amazon.awssdk.utils.BinaryUtils;

public class ServiceIntegrationTest extends AwsIntegrationTestBase {

    private static final String STATE_FIELD_NAME = "state";
    private static final String THING_NAME = "foo";
    private static final String INVALID_THING_NAME = "INVALID_THING_NAME";

    private IoTDataPlaneClient iot;

    private static ByteBuffer getPayloadAsByteBuffer(String payloadString) {
        return ByteBuffer.wrap(payloadString.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonNode getPayloadAsJsonNode(ByteBuffer payload) throws IOException {
        return new ObjectMapper().readTree(BinaryUtils.toStream(payload));
    }

    private static void assertPayloadNonEmpty(ByteBuffer payload) {
        assertThat(payload.capacity(), greaterThan(0));
    }

    /**
     * Asserts that the returned payload has the correct state in the JSON document. It should be
     * exactly what we sent it plus some additional metadata
     *
     * @param originalPayload
     *            ByteBuffer we sent to the service containing just the state
     * @param returnedPayload
     *            ByteBuffer returned by the service containing the state (which should be the same
     *            as what we sent) plus additional metadata in the JSON document
     */
    private static void assertPayloadIsValid(ByteBuffer originalPayload, ByteBuffer returnedPayload) throws Exception {
        JsonNode originalJson = getPayloadAsJsonNode(originalPayload);
        JsonNode returnedJson = getPayloadAsJsonNode(returnedPayload);
        assertEquals(originalJson.get(STATE_FIELD_NAME), returnedJson.get(STATE_FIELD_NAME));
    }

    @Before
    public void setup() throws Exception {
        iot = IoTDataPlaneClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.US_EAST_1).build();
    }

    @Test
    public void publish_ValidTopicAndNonEmptyPayload_DoesNotThrowException() {
        iot.publish(PublishRequest.builder().topic(THING_NAME).payload(ByteBuffer.wrap(new byte[] {1, 2, 3, 4})).build());
    }

    @Test
    public void publish_WithValidTopicAndEmptyPayload_DoesNotThrowException() {
        iot.publish(PublishRequest.builder().topic(THING_NAME).payload(null).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void updateThingShadow_NullPayload_ThrowsServiceException() throws Exception {
        UpdateThingShadowRequest request = UpdateThingShadowRequest.builder().thingName(THING_NAME).payload(null).build();
        iot.updateThingShadow(request);
    }

    @Test(expected = InvalidRequestException.class)
    public void updateThingShadow_MalformedPayload_ThrowsServiceException() throws Exception {
        ByteBuffer payload = getPayloadAsByteBuffer("{ }");
        UpdateThingShadowRequest request = UpdateThingShadowRequest.builder().thingName(THING_NAME).payload(payload).build();
        iot.updateThingShadow(request);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getThingShadow_InvalidThingName_ThrowsException() {
        iot.getThingShadow(GetThingShadowRequest.builder().thingName(INVALID_THING_NAME).build());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void deleteThingShadow_InvalidThing_ThrowsException() {
        DeleteThingShadowResponse result = iot
                .deleteThingShadow(DeleteThingShadowRequest.builder().thingName(INVALID_THING_NAME).build());
        assertPayloadNonEmpty(result.payload());
    }

    @Test
    public void UpdateReadDeleteThing() throws Exception {
        updateThingShadow_ValidRequest_ReturnsValidResponse(THING_NAME);
        getThingShadow_ValidThing_ReturnsThingData(THING_NAME);
        deleteThingShadow_ValidThing_DeletesSuccessfully(THING_NAME);
    }

    private void updateThingShadow_ValidRequest_ReturnsValidResponse(String thingName) throws Exception {
        ByteBuffer originalPayload = getPayloadAsByteBuffer("{ \"state\": {\"reported\":{ \"r\": {}}}}");
        UpdateThingShadowRequest request = UpdateThingShadowRequest.builder().thingName(thingName).payload(originalPayload).build();
        UpdateThingShadowResponse result = iot.updateThingShadow(request);

        // Comes back with some extra metadata so we assert it's bigger than the original
        assertThat(result.payload().capacity(), greaterThan(originalPayload.capacity()));
        assertPayloadIsValid(originalPayload, result.payload());
    }

    private void getThingShadow_ValidThing_ReturnsThingData(String thingName) {
        GetThingShadowRequest request = GetThingShadowRequest.builder().thingName(thingName).build();
        GetThingShadowResponse result = iot.getThingShadow(request);
        assertPayloadNonEmpty(result.payload());
    }

    private void deleteThingShadow_ValidThing_DeletesSuccessfully(String thingName) {
        DeleteThingShadowResponse result = iot.deleteThingShadow(DeleteThingShadowRequest.builder().thingName(thingName).build());
        assertPayloadNonEmpty(result.payload());
    }

}

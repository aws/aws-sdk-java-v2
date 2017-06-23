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

package software.amazon.awssdk.services.sns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import software.amazon.awssdk.services.sns.model.CreatePlatformApplicationRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformApplicationResponse;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.DeleteEndpointRequest;
import software.amazon.awssdk.services.sns.model.DeletePlatformApplicationRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.Endpoint;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesResponse;
import software.amazon.awssdk.services.sns.model.GetPlatformApplicationAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetPlatformApplicationAttributesResponse;
import software.amazon.awssdk.services.sns.model.ListEndpointsByPlatformApplicationRequest;
import software.amazon.awssdk.services.sns.model.ListEndpointsByPlatformApplicationResponse;
import software.amazon.awssdk.services.sns.model.ListPlatformApplicationsRequest;
import software.amazon.awssdk.services.sns.model.ListPlatformApplicationsResponse;
import software.amazon.awssdk.services.sns.model.PlatformApplication;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SetEndpointAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetPlatformApplicationAttributesRequest;

public class MobilePushIntegrationTest extends IntegrationTestBase {

    private String platformAppName = "JavaSDKTestApp" + new Random().nextInt();
    private String platformCredential = "AIzaSyD-4pBAk6M7eveE9dwRFyGv-cfYBPiHRmk";
    private String token =
            "APA91bHXHl9bxaaNvHHWNXKwzzaeAjJnBP3g6ieaGta1aPMgrilr0H-QL4AxUZUJ-1mk0gnLpmeXF0Kg7-9fBXfXHTKzPGlCyT6E6oOfpdwLpcRMxQp5vCPFiFeru9oQylc22HvZSwQTDgmmw9WdNlXMerUPzmoX0w";

    /**
     * Tests for mobile push API
     *
     */
    @Test
    public void testMobilePushOperations() throws InterruptedException {

        String platformApplicationArn = null;
        String endpointArn = null;
        String topicArn = null;

        try {
            CreateTopicResponse createTopicResult = sns.createTopic(CreateTopicRequest.builder().name("TestTopic").build());
            topicArn = createTopicResult.topicArn();

            // List platform applications
            ListPlatformApplicationsResponse listPlatformAppsResult =
                    sns.listPlatformApplications(ListPlatformApplicationsRequest.builder().build());
            int platformAppsCount = listPlatformAppsResult.platformApplications().size();
            for (PlatformApplication platformApp : listPlatformAppsResult.platformApplications()) {
                assertNotNull(platformApp.platformApplicationArn());
                validateAttributes(platformApp.attributes());
            }

            // Create a platform application for GCM.
            Map<String, String> attributes = new HashMap<>();
            attributes.put("PlatformCredential", platformCredential);
            attributes.put("PlatformPrincipal", "NA");
            attributes.put("EventEndpointCreated", topicArn);
            attributes.put("EventEndpointDeleted", topicArn);
            attributes.put("EventEndpointUpdated", topicArn);
            attributes.put("EventDeliveryAttemptFailure", topicArn);
            attributes.put("EventDeliveryFailure", "");
            CreatePlatformApplicationResponse createPlatformAppResult = sns
                    .createPlatformApplication(CreatePlatformApplicationRequest.builder().name(platformAppName)
                                                                               .platform("GCM").attributes(attributes).build());
            assertNotNull(createPlatformAppResult.platformApplicationArn());
            platformApplicationArn = createPlatformAppResult.platformApplicationArn();

            Thread.sleep(5 * 1000);
            listPlatformAppsResult = sns.listPlatformApplications(ListPlatformApplicationsRequest.builder().build());
            assertEquals(platformAppsCount + 1, listPlatformAppsResult.platformApplications().size());

            // Get attributes
            GetPlatformApplicationAttributesResponse platformAttributesResult = sns.getPlatformApplicationAttributes(
                    GetPlatformApplicationAttributesRequest.builder().platformApplicationArn(platformApplicationArn).build());
            validateAttributes(platformAttributesResult.attributes());

            // Set attributes
            attributes.clear();
            attributes.put("EventDeliveryFailure", topicArn);

            sns.setPlatformApplicationAttributes(SetPlatformApplicationAttributesRequest.builder()
                                                                                        .platformApplicationArn(
                                                                                                platformApplicationArn)
                                                                                        .attributes(attributes).build());

            Thread.sleep(1 * 1000);
            // Verify attribute change
            platformAttributesResult = sns.getPlatformApplicationAttributes(
                    GetPlatformApplicationAttributesRequest.builder().platformApplicationArn(platformApplicationArn).build());
            validateAttribute(platformAttributesResult.attributes(), "EventDeliveryFailure", topicArn);

            // Create platform endpoint
            CreatePlatformEndpointResponse createPlatformEndpointResult = sns.createPlatformEndpoint(
                    CreatePlatformEndpointRequest.builder().platformApplicationArn(platformApplicationArn)
                                                 .customUserData("Custom Data").token(token).build());
            assertNotNull(createPlatformEndpointResult.endpointArn());
            endpointArn = createPlatformEndpointResult.endpointArn();

            // List platform endpoints
            Thread.sleep(5 * 1000);
            ListEndpointsByPlatformApplicationResponse listEndpointsResult = sns.listEndpointsByPlatformApplication(
                    ListEndpointsByPlatformApplicationRequest.builder().platformApplicationArn(platformApplicationArn).build());
            assertTrue(listEndpointsResult.endpoints().size() == 1);
            for (Endpoint endpoint : listEndpointsResult.endpoints()) {
                assertNotNull(endpoint.endpointArn());
                validateAttributes(endpoint.attributes());
            }

            // Publish to the endpoint
            PublishResponse publishResult = sns.publish(PublishRequest.builder().message("Mobile push test message")
                                                                    .subject("Mobile Push test subject").targetArn(endpointArn)
                                                                    .build());
            assertNotNull(publishResult.messageId());

            // Get endpoint attributes
            GetEndpointAttributesResponse endpointAttributesResult = sns
                    .getEndpointAttributes(GetEndpointAttributesRequest.builder().endpointArn(endpointArn).build());
            validateAttributes(endpointAttributesResult.attributes());

            // Set endpoint attributes
            attributes.clear();
            attributes.put("CustomUserData", "Updated Custom Data");
            sns.setEndpointAttributes(
                    SetEndpointAttributesRequest.builder().endpointArn(endpointArn).attributes(attributes).build());

            Thread.sleep(1 * 1000);
            // Validate set endpoint attributes
            endpointAttributesResult = sns
                    .getEndpointAttributes(GetEndpointAttributesRequest.builder().endpointArn(endpointArn).build());
            validateAttribute(endpointAttributesResult.attributes(), "CustomUserData", "Updated Custom Data");

        } finally {
            if (platformApplicationArn != null) {
                if (endpointArn != null) {
                    // Delete endpoint
                    sns.deleteEndpoint(DeleteEndpointRequest.builder().endpointArn(endpointArn).build());
                }
                // Delete application platform
                sns.deletePlatformApplication(
                        DeletePlatformApplicationRequest.builder().platformApplicationArn(platformApplicationArn).build());
            }
            if (topicArn != null) {
                // Delete the topic
                sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
            }
        }
    }

    private void validateAttributes(Map<String, String> attributes) {
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            assertNotNull(attribute.getKey());
            assertNotNull(attribute.getValue());
        }
    }

    private void validateAttribute(Map<String, String> attributes, String key, String expectedValue) {
        if (attributes.containsKey(key)) {
            if (attributes.get(key).equals(expectedValue)) {
                return;
            }
            fail(String.format("The key %s didn't have the expected value %s. Actual value : %s ", key, expectedValue,
                               attributes.get(key)));
        }
        fail(String.format("The key %s wasn't present in the Map.", key));
    }
}

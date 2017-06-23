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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.model.AttributePayload;
import software.amazon.awssdk.services.iot.model.CertificateStatus;
import software.amazon.awssdk.services.iot.model.CreateCertificateFromCsrRequest;
import software.amazon.awssdk.services.iot.model.CreateKeysAndCertificateRequest;
import software.amazon.awssdk.services.iot.model.CreateKeysAndCertificateResponse;
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyResponse;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.CreateThingResponse;
import software.amazon.awssdk.services.iot.model.DeleteCertificateRequest;
import software.amazon.awssdk.services.iot.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResponse;
import software.amazon.awssdk.services.iot.model.GetPolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.GetPolicyVersionResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListThingsRequest;
import software.amazon.awssdk.services.iot.model.ListThingsResponse;
import software.amazon.awssdk.services.iot.model.UpdateCertificateRequest;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Integration tests for Iot control plane APIs.
 */
public class IotControlPlaneIntegrationTest extends AwsTestBase {

    private static final String THING_NAME = "java-sdk-thing-" + System.currentTimeMillis();
    private static final Map<String, String> THING_ATTRIBUTES = new HashMap<String, String>();
    private static final String ATTRIBUTE_NAME = "foo";
    private static final String ATTRIBUTE_VALUE = "bar";
    private static final String POLICY_NAME = "java-sdk-iot-policy-" + System.currentTimeMillis();
    private static final String POLICY_DOC = "{\n" +
                                             "  \"Version\": \"2012-10-17\",\n" +
                                             "  \"Statement\": [\n" +
                                             "    {\n" +
                                             "      \"Sid\": \"Stmt1443818583140\",\n" +
                                             "      \"Action\": \"iot:*\",\n" +
                                             "      \"Effect\": \"Deny\",\n" +
                                             "      \"Resource\": \"*\"\n" +
                                             "    }\n" +
                                             "  ]\n" +
                                             "}";
    private static IoTClient client;
    private static String certificateId = null;

    @BeforeClass
    public static void setup() throws IOException {
        setUpCredentials();
        client = IoTClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.US_WEST_2).build();
        THING_ATTRIBUTES.put(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (client != null) {
            client.deleteThing(DeleteThingRequest.builder().thingName(THING_NAME).build());
            client.deletePolicy(DeletePolicyRequest.builder().policyName(POLICY_NAME).build());
            if (certificateId != null) {
                client.deleteCertificate(DeleteCertificateRequest.builder().certificateId(certificateId).build());
            }
        }
    }

    @Test
    public void describe_and_list_thing_returns_created_thing() {

        final CreateThingRequest createReq = CreateThingRequest.builder()
                                                               .thingName(THING_NAME)
                                                               .attributePayload(AttributePayload.builder().attributes(THING_ATTRIBUTES).build())
                                                               .build();
        CreateThingResponse result = client.createThing(createReq);
        Assert.assertNotNull(result.thingArn());
        Assert.assertEquals(THING_NAME, result.thingName());

        final DescribeThingRequest descRequest = DescribeThingRequest.builder().thingName(THING_NAME).build();

        DescribeThingResponse descResult = client.describeThing(descRequest);
        Map<String, String> actualAttributes = descResult.attributes();
        Assert.assertEquals(THING_ATTRIBUTES.size(), actualAttributes.size());
        Assert.assertTrue(actualAttributes.containsKey(ATTRIBUTE_NAME));
        Assert.assertEquals(THING_ATTRIBUTES.get(ATTRIBUTE_NAME), actualAttributes.get(ATTRIBUTE_NAME));

        ListThingsResponse listResult = client.listThings(ListThingsRequest.builder().build());
        Assert.assertFalse(listResult.things().isEmpty());
    }

    @Test
    public void get_policy_returns_created_policy() {

        final CreatePolicyRequest createReq = CreatePolicyRequest.builder().policyName(POLICY_NAME).policyDocument(POLICY_DOC).build();

        CreatePolicyResponse createResult = client.createPolicy(createReq);
        Assert.assertNotNull(createResult.policyArn());
        Assert.assertNotNull(createResult.policyVersionId());


        final GetPolicyVersionRequest request = GetPolicyVersionRequest.builder()
                                                                       .policyName(POLICY_NAME)
                                                                       .policyVersionId(createResult.policyVersionId())
                                                                       .build();

        GetPolicyVersionResponse result = client.getPolicyVersion(request);
        Assert.assertEquals(createResult.policyArn(), result.policyArn());
        Assert.assertEquals(createResult.policyVersionId(), result.policyVersionId());
    }

    @Test
    public void createCertificate_Returns_success() {
        final CreateKeysAndCertificateRequest createReq = CreateKeysAndCertificateRequest.builder().setAsActive(true).build();
        CreateKeysAndCertificateResponse createResult = client.createKeysAndCertificate(createReq);
        Assert.assertNotNull(createResult.certificateArn());
        Assert.assertNotNull(createResult.certificateId());
        Assert.assertNotNull(createResult.certificatePem());
        Assert.assertNotNull(createResult.keyPair());

        certificateId = createResult.certificateId();

        client.updateCertificate(UpdateCertificateRequest.builder()
                                                         .certificateId(certificateId)
                                                         .newStatus(CertificateStatus.REVOKED)
                                                         .build());
    }

    @Test(expected = InvalidRequestException.class)
    public void create_certificate_from_invalid_csr_throws_exception() {
        client.createCertificateFromCsr(CreateCertificateFromCsrRequest.builder()
                                                                       .certificateSigningRequest("invalid-csr-string")
                                                                       .build());
    }
}

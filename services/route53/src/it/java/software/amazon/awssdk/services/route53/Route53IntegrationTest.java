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

package software.amazon.awssdk.services.route53;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeBatch;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.CreateHealthCheckRequest;
import software.amazon.awssdk.services.route53.model.CreateHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.DelegationSet;
import software.amazon.awssdk.services.route53.model.DeleteHealthCheckRequest;
import software.amazon.awssdk.services.route53.model.DeleteHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.DeleteHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.GetChangeRequest;
import software.amazon.awssdk.services.route53.model.GetHealthCheckRequest;
import software.amazon.awssdk.services.route53.model.GetHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.GetHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.HealthCheck;
import software.amazon.awssdk.services.route53.model.HealthCheckConfig;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.HostedZoneConfig;
import software.amazon.awssdk.services.route53.model.ListHostedZonesRequest;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

/**
 * Integration tests that run through the various operations available in the
 * Route 53 API.
 */
public class Route53IntegrationTest extends IntegrationTestBase {

    private static final String COMMENT = "comment";
    private static final String ZONE_NAME = "java.sdk.com.";
    private static final String CALLER_REFERENCE = UUID.randomUUID().toString();
    private static final int PORT_NUM = 22;
    private static final String TYPE = "TCP";
    private static final String IP_ADDRESS = "12.12.12.12";

    /**
     * The ID of the zone we created in this test.
     */
    private static String createdZoneId;

    /**
     * The ID of the change that created our test zone.
     */
    private String createdZoneChangeId;

    /**
     * the ID of the health check.
     */
    private String healthCheckId;


    /**
     * Ensures the HostedZone we create during this test is correctly released.
     */
    @AfterClass
    public static void tearDown() {
        try {
            route53.deleteHostedZone(DeleteHostedZoneRequest.builder().id(createdZoneId).build());
        } catch (Exception e) {
            // Ignored or expected.
        }
    }

    /**
     * Runs through each of the APIs in the Route 53 client to make sure we can
     * correct send requests and unmarshall responses.
     */
    @Test
    public void testRoute53() throws Exception {
        // Create Hosted Zone
        CreateHostedZoneResponse result = route53.createHostedZone(CreateHostedZoneRequest.builder()
                                                                         .name(ZONE_NAME)
                                                                         .callerReference(CALLER_REFERENCE)
                                                                         .hostedZoneConfig(HostedZoneConfig.builder()
                                                                                                       .comment(COMMENT).build()).build()
        );

        createdZoneId = result.hostedZone().id();
        createdZoneChangeId = result.changeInfo().id();

        assertValidCreatedHostedZone(result.hostedZone());
        assertValidDelegationSet(result.delegationSet());
        assertValidChangeInfo(result.changeInfo());
        assertNotNull(result.location());


        // Get Hosted Zone
        GetHostedZoneRequest hostedZoneRequest = GetHostedZoneRequest.builder().id(createdZoneId).build();
        GetHostedZoneResponse hostedZoneResult = route53.getHostedZone(hostedZoneRequest);
        assertValidDelegationSet(hostedZoneResult.delegationSet());
        assertValidCreatedHostedZone(hostedZoneResult.hostedZone());

        // Create a health check
        HealthCheckConfig config = HealthCheckConfig.builder().type("TCP").port(PORT_NUM).ipAddress(IP_ADDRESS).build();
        CreateHealthCheckResponse createHealthCheckResult = route53.createHealthCheck(
                CreateHealthCheckRequest.builder().healthCheckConfig(config).callerReference(CALLER_REFERENCE).build());
        healthCheckId = createHealthCheckResult.healthCheck().id();
        assertNotNull(createHealthCheckResult.location());
        assertValidHealthCheck(createHealthCheckResult.healthCheck());

        // Get the health check back
        GetHealthCheckResponse gealthCheckResult = route53
                .getHealthCheck(GetHealthCheckRequest.builder().healthCheckId(healthCheckId).build());
        assertValidHealthCheck(gealthCheckResult.healthCheck());

        // Delete the health check
        route53.deleteHealthCheck(DeleteHealthCheckRequest.builder().healthCheckId(healthCheckId).build());

        // Get the health check back
        try {
            gealthCheckResult = route53.getHealthCheck(GetHealthCheckRequest.builder().healthCheckId(healthCheckId).build());
            fail();
        } catch (SdkServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.errorCode());
            assertNotNull(e.errorType());
        }

        // List Hosted Zones
        List<HostedZone> hostedZones = route53.listHostedZones(ListHostedZonesRequest.builder().build()).hostedZones();
        assertTrue(hostedZones.size() > 0);
        for (HostedZone hostedZone : hostedZones) {
            assertNotNull(hostedZone.callerReference());
            assertNotNull(hostedZone.id());
            assertNotNull(hostedZone.name());
        }


        // List Resource Record Sets
        List<ResourceRecordSet> resourceRecordSets = route53.listResourceRecordSets(
                ListResourceRecordSetsRequest.builder().hostedZoneId(createdZoneId).build()).resourceRecordSets();
        assertTrue(resourceRecordSets.size() > 0);
        ResourceRecordSet existingResourceRecordSet = resourceRecordSets.get(0);
        for (ResourceRecordSet rrset : resourceRecordSets) {
            assertNotNull(rrset.name());
            assertNotNull(rrset.type());
            assertNotNull(rrset.ttl());
            assertTrue(rrset.resourceRecords().size() > 0);
        }


        // Get Change
        ChangeInfo changeInfo = route53.getChange(GetChangeRequest.builder().id(createdZoneChangeId).build()).changeInfo();
        assertTrue(changeInfo.id().endsWith(createdZoneChangeId));
        assertValidChangeInfo(changeInfo);


        // Change Resource Record Sets
        ResourceRecordSet newResourceRecordSet = ResourceRecordSet.builder()
                .name(ZONE_NAME)
                .resourceRecords(existingResourceRecordSet.resourceRecords())
                .ttl(existingResourceRecordSet.ttl() + 100)
                .type(existingResourceRecordSet.type())
                .build();

        changeInfo = route53.changeResourceRecordSets(ChangeResourceRecordSetsRequest.builder()
                                                              .hostedZoneId(createdZoneId)
                                                              .changeBatch(ChangeBatch.builder().comment(COMMENT)
                                                                                       .changes(Change.builder().action(
                                                                                               ChangeAction.DELETE)
                                                                                                            .resourceRecordSet(
                                                                                                                    existingResourceRecordSet).build(),
                                                                                                    Change.builder().action(
                                                                                                            ChangeAction.CREATE)
                                                                                                            .resourceRecordSet(
                                                                                                                    newResourceRecordSet).build()).build()
                                                              ).build()).changeInfo();
        assertValidChangeInfo(changeInfo);

        // Add a weighted Resource Record Set so we can reproduce the bug reported by customers
        // when they provide SetIdentifier containing special characters.
        String specialChars = "&<>'\"";
        newResourceRecordSet =ResourceRecordSet.builder()
                .name("weighted." + ZONE_NAME)
                .type(RRType.CNAME)
                .setIdentifier(specialChars)
                .weight(0L)
                .ttl(1000L)
                .resourceRecords(ResourceRecord.builder().value("www.example.com").build())
                .build();
        changeInfo = route53.changeResourceRecordSets(ChangeResourceRecordSetsRequest.builder()
                                                              .hostedZoneId(createdZoneId)
                                                              .changeBatch(
                                                                      ChangeBatch.builder().comment(COMMENT).changes(
                                                                              Change.builder().action(ChangeAction.CREATE)
                                                                                      .resourceRecordSet(
                                                                                              newResourceRecordSet).build()).build()).build()
        ).changeInfo();
        assertValidChangeInfo(changeInfo);

        // Clear up the RR Set
        changeInfo = route53.changeResourceRecordSets(ChangeResourceRecordSetsRequest.builder()
                                                              .hostedZoneId(createdZoneId)
                                                              .changeBatch(
                                                                      ChangeBatch.builder().comment(COMMENT).changes(
                                                                              Change.builder().action(ChangeAction.DELETE)
                                                                                      .resourceRecordSet(
                                                                                              newResourceRecordSet).build()).build()).build()
        ).changeInfo();

        // Delete Hosted Zone
        DeleteHostedZoneResponse deleteHostedZoneResult = route53.deleteHostedZone(DeleteHostedZoneRequest.builder().id(createdZoneId).build());
        assertValidChangeInfo(deleteHostedZoneResult.changeInfo());
    }


    /**
     * Asserts that the specified HostedZone is valid and represents the same
     * HostedZone that we initially created at the very start of this test.
     *
     * @param hostedZone The hosted zone to test.
     */
    private void assertValidCreatedHostedZone(HostedZone hostedZone) {
        assertEquals(CALLER_REFERENCE, hostedZone.callerReference());
        assertEquals(ZONE_NAME, hostedZone.name());
        assertNotNull(hostedZone.id());
        assertEquals(COMMENT, hostedZone.config().comment());
    }

    /**
     * Asserts that the specified DelegationSet is valid.
     *
     * @param delegationSet The delegation set to test.
     */
    private void assertValidDelegationSet(DelegationSet delegationSet) {
        assertTrue(delegationSet.nameServers().size() > 0);
        for (String server : delegationSet.nameServers()) {
            assertNotNull(server);
        }
    }

    /**
     * Asserts that the specified ChangeInfo is valid.
     *
     * @param change The ChangeInfo object to test.
     */
    private void assertValidChangeInfo(ChangeInfo change) {
        assertNotNull(change.id());
        assertNotNull(change.status());
        assertNotNull(change.submittedAt());
    }

    private void assertValidHealthCheck(HealthCheck healthCheck) {
        assertNotNull(CALLER_REFERENCE, healthCheck.callerReference());
        assertNotNull(healthCheck.id());
        assertEquals(PORT_NUM, healthCheck.healthCheckConfig().port().intValue());
        assertEquals(TYPE, healthCheck.healthCheckConfig().typeString());
        assertEquals(IP_ADDRESS, healthCheck.healthCheckConfig().ipAddress());
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() throws SdkServiceException {
        SdkGlobalTime.setGlobalTimeOffset(3600);
        Route53Client clockSkewClient = Route53Client.builder()
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .build();
        clockSkewClient.listHostedZones(ListHostedZonesRequest.builder().build());
        assertTrue("Clockskew is fixed!", SdkGlobalTime.getGlobalTimeOffset() < 60);
    }
}

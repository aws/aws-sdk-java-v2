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

package software.amazon.awssdk.services.elasticloadbalancing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.EC2Client;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.ConnectionDraining;
import software.amazon.awssdk.services.elasticloadbalancing.model.CreateLBCookieStickinessPolicyRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.CrossZoneLoadBalancing;
import software.amazon.awssdk.services.elasticloadbalancing.model.DeleteLoadBalancerListenersRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DeleteLoadBalancerPolicyRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeLoadBalancerAttributesResponse;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeLoadBalancerPoliciesRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeLoadBalancerPolicyTypesRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DisableAvailabilityZonesForLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.EnableAvailabilityZonesForLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.HealthCheck;
import software.amazon.awssdk.services.elasticloadbalancing.model.Instance;
import software.amazon.awssdk.services.elasticloadbalancing.model.InstanceState;
import software.amazon.awssdk.services.elasticloadbalancing.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancing.model.ListenerDescription;
import software.amazon.awssdk.services.elasticloadbalancing.model.LoadBalancerAttributes;
import software.amazon.awssdk.services.elasticloadbalancing.model.LoadBalancerDescription;
import software.amazon.awssdk.services.elasticloadbalancing.model.ModifyLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.PolicyDescription;
import software.amazon.awssdk.services.elasticloadbalancing.model.PolicyTypeDescription;
import software.amazon.awssdk.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.SetLoadBalancerListenerSSLCertificateRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.SetLoadBalancerPoliciesOfListenerRequest;
import software.amazon.awssdk.services.iam.IAMClient;
import software.amazon.awssdk.services.iam.model.ListServerCertificatesRequest;
import software.amazon.awssdk.services.iam.model.ServerCertificateMetadata;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

/**
 * Integration tests for the Elastic Load Balancing client.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class ElbIntegrationTest extends AwsIntegrationTestBase {

    /** AMI used for tests that require an EC2 instance. */
    private static final String AMI_ID = "ami-7f418316";

    /** Protocol value used in LB requests. */
    private static final String PROTOCOL = "HTTP";

    /** AZs used for LB requests. */
    private static final String AVAILABILITY_ZONE_1 = "us-east-1a";
    private static final String AVAILABILITY_ZONE_2 = "us-east-1b";

    /**
     * Region to run tests against. Must be us-east-1 since AZ's are hardcoded
     */
    private static final Region REGION = Region.US_EAST_1;

    /** The ELB client used in these tests. */
    private static ElasticLoadBalancingClient elb;

    /** The EC2 client used to start an instance for the tests requiring one. */
    private static EC2Client ec2;

    /** IAM client used to retrieve certificateArn. */
    private static IAMClient iam;

    /** Existing SSL certificate ARN in IAM. */
    private static String certificateArn;

    /** The name of a load balancer created and tested by these tests. */
    private String loadBalancerName;

    /** The ID of an EC2 instance created and used by these tests. */
    private String instanceId;

    /**
     * dns name for the new created load balancer
     */
    private String dnsName;

    /**
     * Loads the AWS account info for the integration tests and creates an EC2
     * client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws IOException {
        elb = ElasticLoadBalancingClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(REGION)
                .build();
        ec2 = EC2Client.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(REGION)
                .build();
        iam = IAMClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(Region.AWS_GLOBAL)
                .build();

        List<ServerCertificateMetadata> serverCertificates = iam.listServerCertificates(
                ListServerCertificatesRequest.builder().build()).serverCertificateMetadataList();
        if (!serverCertificates.isEmpty()) {
            certificateArn = serverCertificates.get(0).arn();
        }
    }

    /**
     * Create LoadBalancer resource before each unit test
     */
    @Before
    public void createDefaultLoadBalancer() {
        loadBalancerName = "integ-test-lb-" + System.currentTimeMillis();
        Listener expectedListener = Listener.builder().instancePort(8080)
                .loadBalancerPort(80)
                .protocol(PROTOCOL)
                .build();

        // Create a load balancer
        dnsName = elb.createLoadBalancer(
                CreateLoadBalancerRequest.builder()
                        .loadBalancerName(loadBalancerName)
                        .availabilityZones(AVAILABILITY_ZONE_1)
                        .listeners(expectedListener).build()).dnsName();

        assertThat(dnsName, not(isEmptyOrNullString()));
    }

    /** Release any resources created by this test. */
    @After
    public void tearDown() throws Exception {
        if (loadBalancerName != null) {
            try {
                elb.deleteLoadBalancer(DeleteLoadBalancerRequest.builder()
                                               .loadBalancerName(loadBalancerName).build());
            } catch (Exception e) {
                // Ignored or expected.
            }
        }
        if (instanceId != null) {
            try {
                ec2.terminateInstances(TerminateInstancesRequest.builder()
                                               .instanceIds(instanceId).build());
            } catch (Exception e) {
                // Ignored or expected.
            }
        }
    }

    /**
     * Tests the ELB operations that require a real EC2 instance.
     */
    @Test
    public void testLoadBalancerInstanceOperations() throws Exception {
        // Start up an EC2 instance to register with our LB
        RunInstancesRequest runInstancesRequest = RunInstancesRequest.builder()
                .placement(
                        Placement.builder()
                                .availabilityZone(AVAILABILITY_ZONE_1).build())
                .imageId(AMI_ID).minCount(1).maxCount(1).build();
        instanceId = ec2.runInstances(runInstancesRequest).reservation()
                        .instances().get(0).instanceId();

        // Register it with our load balancer
        List<Instance> instances = elb.registerInstancesWithLoadBalancer(
                RegisterInstancesWithLoadBalancerRequest.builder().instances(
                        Instance.builder().instanceId(instanceId).build())
                                                              .loadBalancerName(loadBalancerName).build()).instances();
        assertEquals(1, instances.size());
        assertEquals(instanceId, instances.get(0).instanceId());

        // Describe it's health
        List<InstanceState> instanceStates = elb.describeInstanceHealth(
                DescribeInstanceHealthRequest.builder().instances(
                        Instance.builder().instanceId(instanceId).build())
                                                   .loadBalancerName(loadBalancerName).build())
                                                .instanceStates();
        assertEquals(1, instanceStates.size());
        assertThat(instanceStates.get(0).description(), not(isEmptyOrNullString()));
        assertEquals(instanceId, instanceStates.get(0).instanceId());
        assertThat(instanceStates.get(0).reasonCode(), not(isEmptyOrNullString()));
        assertThat(instanceStates.get(0).state(), not(isEmptyOrNullString()));

        // Deregister it
        instances = elb.deregisterInstancesFromLoadBalancer(
                DeregisterInstancesFromLoadBalancerRequest.builder().instances(
                        Instance.builder().instanceId(instanceId).build())
                                                                .loadBalancerName(loadBalancerName).build()).instances();
        assertEquals(0, instances.size());
    }

    /**
     * Tests that the ELB client can call the basic load balancer operations (no
     * operations requiring a real EC2 instance).
     */
    @Test
    public void testLoadBalancerOperations() throws Exception {
        // Configure health checks
        HealthCheck expectedHealthCheck = HealthCheck.builder().interval(120)
                                                           .target("HTTP:80/ping").timeout(60)
                                                           .unhealthyThreshold(9).healthyThreshold(10).build();
        HealthCheck createdHealthCheck = elb.configureHealthCheck(
                ConfigureHealthCheckRequest.builder().loadBalancerName(
                        loadBalancerName).healthCheck(expectedHealthCheck).build())
                                            .healthCheck();
        assertEquals(expectedHealthCheck.healthyThreshold(),
                     createdHealthCheck.healthyThreshold());
        assertEquals(expectedHealthCheck.interval(),
                     createdHealthCheck.interval());
        assertEquals(expectedHealthCheck.target(),
                     createdHealthCheck.target());
        assertEquals(expectedHealthCheck.timeout(),
                     createdHealthCheck.timeout());
        assertEquals(expectedHealthCheck.unhealthyThreshold(),
                     createdHealthCheck.unhealthyThreshold());

        // Describe
        List<LoadBalancerDescription> loadBalancerDescriptions = elb
                .describeLoadBalancers(
                        DescribeLoadBalancersRequest.builder()
                                .loadBalancerNames(loadBalancerName).build())
                .loadBalancerDescriptions();
        assertEquals(1, loadBalancerDescriptions.size());
        LoadBalancerDescription loadBalancer = loadBalancerDescriptions.get(0);
        assertEquals(loadBalancerName, loadBalancer.loadBalancerName());
        assertEquals(1, loadBalancer.availabilityZones().size());
        assertTrue(loadBalancer.availabilityZones().contains(
                AVAILABILITY_ZONE_1));
        assertNotNull(loadBalancer.createdTime());
        assertEquals(dnsName, loadBalancer.dnsName());
        assertEquals(expectedHealthCheck.target(), loadBalancer
                .healthCheck().target());
        assertTrue(loadBalancer.instances().isEmpty());
        assertEquals(1, loadBalancer.listenerDescriptions().size());
        assertEquals(8080, loadBalancer.listenerDescriptions().get(0)
                                       .listener().instancePort(), 0.0);
        assertEquals(80, loadBalancer.listenerDescriptions().get(0)
                                     .listener().loadBalancerPort(), 0.0);
        assertEquals(PROTOCOL, loadBalancer.listenerDescriptions().get(0)
                                           .listener().protocol());
        assertEquals(loadBalancerName, loadBalancer.loadBalancerName());
        assertNotNull(loadBalancer.sourceSecurityGroup());
        assertNotNull(loadBalancer.sourceSecurityGroup().groupName());
        assertNotNull(loadBalancer.sourceSecurityGroup().ownerAlias());

        // Enabled AZs
        List<String> availabilityZones = elb
                .enableAvailabilityZonesForLoadBalancer(
                        EnableAvailabilityZonesForLoadBalancerRequest.builder()
                                .loadBalancerName(loadBalancerName)
                                .availabilityZones(AVAILABILITY_ZONE_2).build())
                .availabilityZones();
        assertEquals(2, availabilityZones.size());
        assertTrue(availabilityZones.contains(AVAILABILITY_ZONE_1));
        assertTrue(availabilityZones.contains(AVAILABILITY_ZONE_2));

        /*
         * Enabling and disabling AZs is a relatively expensive operation that
         * kicks of longer running workflow processes, so we don't want to
         * enable and disable AZs back to back. This small sleep ensures that
         * these tests aren't antagonistic for the ELB service and don't cause
         * problems for that team.
         */
        Thread.sleep(1000 * 10);

        // Disable AZs
        availabilityZones = elb.disableAvailabilityZonesForLoadBalancer(
                DisableAvailabilityZonesForLoadBalancerRequest.builder()
                        .loadBalancerName(loadBalancerName)
                        .availabilityZones(AVAILABILITY_ZONE_2).build())
                               .availabilityZones();
        assertEquals(1, availabilityZones.size());
        assertTrue(availabilityZones.contains(AVAILABILITY_ZONE_1));
        assertFalse(availabilityZones.contains(AVAILABILITY_ZONE_2));

        // Create a new SSL listener
        if (certificateArn != null) {
            elb.createLoadBalancerListeners(CreateLoadBalancerListenersRequest.builder()
                                                    .loadBalancerName(loadBalancerName).listeners(
                            Listener.builder().instancePort(8181)
                                          .loadBalancerPort(443).protocol("SSL")
                                          .sslCertificateId(certificateArn).build()).build());
            Thread.sleep(1000 * 5);
            List<ListenerDescription> listenerDescriptions = elb
                    .describeLoadBalancers(
                            DescribeLoadBalancersRequest.builder()
                                    .loadBalancerNames(loadBalancerName).build())
                    .loadBalancerDescriptions().get(0).listenerDescriptions();
            assertEquals(2, listenerDescriptions.size());
            ListenerDescription sslListener = null;
            for (ListenerDescription listener : listenerDescriptions) {
                if (listener.listener().loadBalancerPort() == 443) {
                    sslListener = listener;
                }
            }
            assertEquals(certificateArn, sslListener.listener()
                                                    .sslCertificateId());
        }

        // Describe LB Policy Types
        List<PolicyTypeDescription> policyTypeDescriptions = elb
                .describeLoadBalancerPolicyTypes(DescribeLoadBalancerPolicyTypesRequest.builder().build()).policyTypeDescriptions();
        assertTrue(policyTypeDescriptions.size() > 0);
        assertNotNull(policyTypeDescriptions.get(0).policyTypeName());
        assertTrue(policyTypeDescriptions.get(0)
                                         .policyAttributeTypeDescriptions().size() > 0);
        assertNotNull(policyTypeDescriptions.get(0)
                                            .policyAttributeTypeDescriptions().get(0).attributeName());
        assertNotNull(policyTypeDescriptions.get(0)
                                            .policyAttributeTypeDescriptions().get(0).attributeType());
        assertNotNull(policyTypeDescriptions.get(0)
                                            .policyAttributeTypeDescriptions().get(0).cardinality());


        // Modify LB Attributes
        elb.modifyLoadBalancerAttributes(ModifyLoadBalancerAttributesRequest.builder()
                                                 .loadBalancerName(loadBalancerName)
                                                 .loadBalancerAttributes(
                                                         LoadBalancerAttributes.builder()
                                                                 .crossZoneLoadBalancing(CrossZoneLoadBalancing.builder()
                                                                                                     .enabled(true).build()).build()).build());

        // Describe LB Attributes
        DescribeLoadBalancerAttributesResponse describeLoadBalancerAttributesResult = elb
                .describeLoadBalancerAttributes(DescribeLoadBalancerAttributesRequest.builder()
                                                        .loadBalancerName(loadBalancerName).build());
        CrossZoneLoadBalancing returnedCrossZoneLoadBalancing = describeLoadBalancerAttributesResult
                .loadBalancerAttributes().crossZoneLoadBalancing();
        assertTrue(returnedCrossZoneLoadBalancing.enabled());

        if (certificateArn != null) {
            // Set the SSL certificate for an existing listener
            elb.setLoadBalancerListenerSSLCertificate(SetLoadBalancerListenerSSLCertificateRequest.builder()
                                                              .loadBalancerName(loadBalancerName)
                                                              .loadBalancerPort(443)
                                                              .sslCertificateId(certificateArn).build());

            // Delete the SSL listener
            Thread.sleep(1000 * 5);
            elb.deleteLoadBalancerListeners(DeleteLoadBalancerListenersRequest.builder()
                                                    .loadBalancerName(loadBalancerName).loadBalancerPorts(
                            443).build());
        }

    }

    /**
     * Tests if a specified load balancer contains a listener using the
     * specified policy.
     *
     * @param loadBalancerName
     *            The name of the load balancer to test.
     * @param policyName
     *            The name of the policy to look for.
     *
     * @return True if the specified load balancer contains a listener using the
     *         specified policy.
     */
    private boolean doesLoadBalancerHaveListenerWithPolicy(
            String loadBalancerName, String policyName) {
        List<LoadBalancerDescription> loadBalancers = elb
                .describeLoadBalancers(
                        DescribeLoadBalancersRequest.builder()
                                .loadBalancerNames(loadBalancerName).build())
                .loadBalancerDescriptions();
        if (loadBalancers.isEmpty()) {
            fail("Unknown load balancer: " + loadBalancerName);
        }
        List<ListenerDescription> listeners = loadBalancers.get(0)
                                                           .listenerDescriptions();
        for (ListenerDescription listener : listeners) {
            if (listener.policyNames().contains(policyName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests the new connection draining parameter. Initially when the load
     * balancer is created, the connection draining must be FALSE. Then the
     * <code>ModifyLoadBalancerAttributes</code> API is used to enable the
     * connection draining attribute. Asserts that the connection draining
     * attribute is set to TRUE by retrieving the value from the load balancer
     * through the describe load balancer attribute API.
     */
    @Test
    public void testConnectionDraining() {
        int timeout = 20;

        // Retrieves the load balancer attributes.
        DescribeLoadBalancerAttributesRequest describeLoadBalancerRequest = DescribeLoadBalancerAttributesRequest.builder()
                .loadBalancerName(loadBalancerName).build();

        DescribeLoadBalancerAttributesResponse result = elb
                .describeLoadBalancerAttributes(describeLoadBalancerRequest);

        // Connection draining must be FALSE by default.
        assertFalse(result.loadBalancerAttributes().connectionDraining()
                          .enabled());

        // Enable the connection draining attribute
        LoadBalancerAttributes loadBalancerAttributes = LoadBalancerAttributes.builder()
                .connectionDraining(ConnectionDraining.builder().enabled(
                        Boolean.TRUE).timeout(timeout).build()).build();
        elb.modifyLoadBalancerAttributes(ModifyLoadBalancerAttributesRequest.builder()
                                                 .loadBalancerName(loadBalancerName)
                                                 .loadBalancerAttributes(loadBalancerAttributes).build());

        result = elb
                .describeLoadBalancerAttributes(DescribeLoadBalancerAttributesRequest.builder()
                                                        .loadBalancerName(loadBalancerName).build());

        // Connection draining must be TRUE now.
        assertTrue(result.loadBalancerAttributes().connectionDraining()
                         .enabled());
    }

    /**
     * Test given a null policyNames, the policies attached to a Listener are removed.
     */
    @Test
    public void testSetLoadBalancerPoliciesOfListener() {
        // Create LB stickiness policy
        String policyName = "java-sdk-policy-" + System.currentTimeMillis();
        elb.createLBCookieStickinessPolicy(CreateLBCookieStickinessPolicyRequest.builder().loadBalancerName(
                loadBalancerName).policyName(policyName).build());

        // Attach the policy to a listener
        elb.setLoadBalancerPoliciesOfListener(SetLoadBalancerPoliciesOfListenerRequest.builder().loadBalancerName(
                loadBalancerName).loadBalancerPort(80).policyNames(policyName).build());
        assertTrue(doesLoadBalancerHaveListenerWithPolicy(loadBalancerName,
                                                          policyName));

        // Describe LB Policies
        List<PolicyDescription> policyDescriptions = elb
                .describeLoadBalancerPolicies(
                        DescribeLoadBalancerPoliciesRequest.builder()
                                .loadBalancerName(loadBalancerName).build())
                .policyDescriptions();
        assertTrue(policyDescriptions.size() > 0);
        assertTrue(policyDescriptions.get(0).policyAttributeDescriptions()
                                     .size() > 0);
        assertNotNull(policyDescriptions.get(0).policyName());
        assertNotNull(policyDescriptions.get(0).policyTypeName());

        // Remove the policy from the listener
        elb.setLoadBalancerPoliciesOfListener(SetLoadBalancerPoliciesOfListenerRequest.builder().loadBalancerName(loadBalancerName).loadBalancerPort(80).build());
        assertFalse(doesLoadBalancerHaveListenerWithPolicy(loadBalancerName,
                                                           policyName));

        // Delete the policy
        elb.deleteLoadBalancerPolicy(DeleteLoadBalancerPolicyRequest.builder().loadBalancerName(loadBalancerName).policyName(policyName).build());
    }
}

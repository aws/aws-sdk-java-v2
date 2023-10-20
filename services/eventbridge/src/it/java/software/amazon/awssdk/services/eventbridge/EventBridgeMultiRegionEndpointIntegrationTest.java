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

package software.amazon.awssdk.services.eventbridge;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.model.DescribeEndpointResponse;
import software.amazon.awssdk.services.eventbridge.model.EndpointState;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.ReplicationState;
import software.amazon.awssdk.services.eventbridge.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.CreateHealthCheckResponse;
import software.amazon.awssdk.services.route53.model.HealthCheckType;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;
import software.amazon.awssdk.utils.Logger;

class EventBridgeMultiRegionEndpointIntegrationTest extends AwsIntegrationTestBase {
    private static final Logger log = Logger.loggerFor(EventBridgeMultiRegionEndpointIntegrationTest.class);
    private static final String RESOURCE_PREFIX = "java-sdk-integ-test-eb-mrep-";
    private static final Region PRIMARY_REGION = Region.US_EAST_1;
    private static final Region FAILOVER_REGION = Region.US_WEST_2;

    private CapturingExecutionInterceptor interceptor;
    private Route53Client route53;
    private EventBridgeClient primaryEventBridge;
    private EventBridgeClient failoverEventBridge;

    @BeforeEach
    public void setup() {
        interceptor = new CapturingExecutionInterceptor();
        route53 = Route53Client.builder()
                               .credentialsProvider(getCredentialsProvider())
                               .region(Region.AWS_GLOBAL)
                               .build();
        primaryEventBridge = createEventBridgeClient(PRIMARY_REGION);
        failoverEventBridge = createEventBridgeClient(FAILOVER_REGION);
    }

    private EventBridgeClient createEventBridgeClient(Region region) {
        return EventBridgeClient.builder()
                                .region(region)
                                .credentialsProvider(getCredentialsProvider())
                                .overrideConfiguration(ClientOverrideConfiguration
                                                           .builder()
                                                           .addExecutionInterceptor(interceptor)
                                                           .build())
                                .build();
    }

    @Test
    void testPutEventsToMultiRegionEndpoint() {
        String endpointId = prepareEndpoint();
        PutEventsResponse putEventsResponse = putEvents(endpointId);
        // Assert the endpointId parameter was used to infer the request endpoint 
        assertThat(lastRequest().host()).isEqualTo(endpointId + ".endpoint.events.amazonaws.com");
        // Assert the request was signed with SigV4a
        assertThat(lastRequest().firstMatchingHeader("Authorization").get()).contains("AWS4-ECDSA-P256-SHA256");
        assertThat(lastRequest().firstMatchingHeader("X-Amz-Region-Set")).hasValue("*");
        // Assert the request succeeded normally
        if (putEventsResponse.failedEntryCount() != 0) {
            throw new AssertionError("Failed to put events: " + putEventsResponse);
        }
    }

    /**
     * Execute all the steps needed to prepare a global endpoint, creating the relevant resources if needed.
     */
    private String prepareEndpoint() {
        String healthCheckId = getOrCreateHealthCheck();
        log.info(() -> "healthCheckId: " + healthCheckId);

        String primaryEventBusArn = getOrCreateEventBus(primaryEventBridge);
        log.info(() -> "primaryEventBusArn: " + primaryEventBusArn);

        String failoverEventBusArn = getOrCreateEventBus(failoverEventBridge);
        log.info(() -> "failoverEventBusArn: " + failoverEventBusArn);

        String endpointName = getOrCreateMultiRegionEndpoint(healthCheckId, primaryEventBusArn, failoverEventBusArn);
        log.info(() -> "endpointName: " + endpointName);

        String endpointId = getOrAwaitEndpointId(endpointName);
        log.info(() -> "endpointId: " + endpointId);
        assertThat(endpointId).isNotBlank();
        return endpointId;
    }

    /**
     * Returns the Route 53 healthcheck ID used for testing, creating a healthcheck if needed.
     * <p>
     * The healthcheck is created as {@code DISABLED}, meaning it always reports as healthy.
     * <p>
     * (A healthcheck is required to create an EventBridge multi-region endpoint.)
     */
    private String getOrCreateHealthCheck() {
        URI primaryEndpoint = EventBridgeClient.serviceMetadata().endpointFor(PRIMARY_REGION);
        CreateHealthCheckResponse createHealthCheckResponse = route53.createHealthCheck(r -> r
            .callerReference(resourceName("monitor"))
            .healthCheckConfig(hcc -> hcc
                .type(HealthCheckType.TCP)
                .port(443)
                .fullyQualifiedDomainName(primaryEndpoint.toString())
                .disabled(true)));
        return createHealthCheckResponse.healthCheck().id();
    }

    /**
     * Returns the event bus ARN used for testing, creating an event bus if needed.
     * <p>
     * (An event bus is required to create an EventBridge multi-region endpoint.)
     */
    private String getOrCreateEventBus(EventBridgeClient eventBridge) {
        String eventBusName = resourceName("eventBus");
        try {
            return eventBridge.createEventBus(r -> r.name(eventBusName))
                              .eventBusArn();
        } catch (ResourceAlreadyExistsException ignored) {
            log.debug(() -> "Event bus " + eventBusName + " already exists");
            return eventBridge.describeEventBus(r -> r.name(eventBusName))
                              .arn();
        }
    }

    /**
     * Returns the name of the multi-region endpoint used for testing, creating an endpoint if needed.
     */
    private String getOrCreateMultiRegionEndpoint(String healthCheckId, String primaryEventBusArn, String failoverEventBusArn) {
        String endpointName = resourceName("endpoint");
        try {
            primaryEventBridge.createEndpoint(r -> r
                .name(endpointName)
                .description("Used for SDK Acceptance Testing")
                .eventBuses(eb -> eb.eventBusArn(primaryEventBusArn),
                            eb -> eb.eventBusArn(failoverEventBusArn))
                .routingConfig(rc -> rc
                    .failoverConfig(fc -> fc
                        .primary(p -> p.healthCheck("arn:aws:route53:::healthcheck/" + healthCheckId))
                        .secondary(s -> s.route(FAILOVER_REGION.id()))))
                .replicationConfig(rc -> rc.state(ReplicationState.DISABLED)));
        } catch (ResourceAlreadyExistsException ignored) {
            log.debug(() -> "Endpoint " + endpointName + " already exists");
        }
        return endpointName;
    }

    /**
     * Returns the endpoint ID associated with the given endpoint name, waiting for the endpoint to finish creating if needed.
     */
    private String getOrAwaitEndpointId(String endpointName) {
        DescribeEndpointResponse response =
            Waiter.run(() -> primaryEventBridge.describeEndpoint(r -> r.name(endpointName)))
                  .until(ep -> ep.state() != EndpointState.CREATING)
                  .orFailAfter(Duration.ofMinutes(2));
        assertThat(response.state()).isEqualTo(EndpointState.ACTIVE);
        return response.endpointId();
    }

    /**
     * Put test events to the given endpoint ID, which is expected to override the request's endpoint and to sign the request with
     * SigV4a.
     */
    private PutEventsResponse putEvents(String endpointId) {
        return primaryEventBridge.putEvents(r -> r
            .endpointId(endpointId)
            .entries(e -> e
                .eventBusName(resourceName("eventBus"))
                .resources("resource1", "resource2")
                .source("com.mycompany.myapp")
                .detailType("myDetailType")
                .detail("{ \"key1\": \"value1\", \"key2\": \"value2\" }")));
    }

    /**
     * Return a test-friendly name for a given named resource.
     */
    private String resourceName(String suffix) {
        return RESOURCE_PREFIX + suffix;
    }

    /**
     * Get the last request that was sent with the {@link EventBridgeClient}.
     */
    private SdkHttpRequest lastRequest() {
        return interceptor.beforeTransmission;
    }

    /**
     * Captures {@link SdkHttpRequest}s and saves them to then assert against.
     */
    public static class CapturingExecutionInterceptor implements ExecutionInterceptor {
        private SdkHttpRequest beforeTransmission;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.beforeTransmission = context.httpRequest();
        }
    }
}

package software.amazon.awssdk.services.eventbridge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE;
import static software.amazon.awssdk.regions.RegionScope.GLOBAL;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.EndpointTag;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceEndpointKey;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;

class MultiRegionEndpointInterceptorTest {

    MultiRegionEndpointInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new MultiRegionEndpointInterceptor();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/MultiRegionEndpointInterceptorTestCases.csv", numLinesToSkip = 1)
    void csvTestCases(Region region,
                      boolean useFips,
                      boolean useDualstack,
                      URI overrideEndpoint,
                      String endpointId,
                      boolean expectSigV4aEnabled,
                      String expectedEndpoint,
                      String expectedErrorSubstring) {
        if ("ε".equals(endpointId)) {
            endpointId = "";
        }

        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AwsExecutionAttribute.AWS_REGION, region);
        attributes.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, useFips);
        attributes.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, useDualstack);
        if (overrideEndpoint != null) {
            attributes.putAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT, overrideEndpoint);
            attributes.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, true);
        }

        PutEventsRequest sdkRequest = PutEventsRequest.builder()
                                                      .endpointId(endpointId)
                                                      .build();

        URI endpoint = resolveInitialEndpoint(overrideEndpoint, region, useFips, useDualstack);

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .protocol(Protocol.HTTPS.toString())
                                                           .method(SdkHttpMethod.POST)
                                                           .host(endpoint.toString())
                                                           .build();

        Context.ModifyHttpRequest ctx = mockContext(sdkRequest, httpRequest);

        Supplier<SdkRequest> modifyRequest = () -> interceptor.modifyRequest(ctx, attributes);
        Supplier<SdkHttpRequest> modifyHttpRequest = () -> interceptor.modifyHttpRequest(ctx, attributes);

        if (expectedEndpoint != null) {
            SdkRequest result = modifyRequest.get();
            if (expectSigV4aEnabled) {
                assertThat(attributes.getAttribute(SIGNING_REGION_SCOPE)).isEqualTo(GLOBAL);
                assertThat(result.overrideConfiguration().get().signer().get()).isInstanceOf(AwsCrtV4aSigner.class);
            } else {
                assertThat(attributes.getAttribute(SIGNING_REGION_SCOPE)).isNull();
                assertThat(result.overrideConfiguration()).isNotPresent();
            }
            assertThat(modifyHttpRequest.get())
                .extracting(SdkHttpRequest::host)
                .isEqualTo(expectedEndpoint);
        } else {
            assertThatThrownBy(modifyRequest::get)
                .hasMessageContaining(expectedErrorSubstring);
        }
    }

    private static URI resolveInitialEndpoint(URI overrideEndpoint, Region region, boolean useFips, boolean useDualstack) {
        if (overrideEndpoint != null) {
            return overrideEndpoint;
        }
        List<EndpointTag> endpointTags = new ArrayList<>();
        if (useFips) {
            endpointTags.add(EndpointTag.FIPS);
        }
        if (useDualstack) {
            endpointTags.add(EndpointTag.DUALSTACK);
        }
        return EventBridgeClient.serviceMetadata().endpointFor(ServiceEndpointKey.builder()
                                                                                 .region(region)
                                                                                 .tags(endpointTags)
                                                                                 .build());
    }

    private static Context.ModifyHttpRequest mockContext(PutEventsRequest sdkRequest, SdkHttpFullRequest request) {
        Context.ModifyHttpRequest ctx = mock(Context.ModifyHttpRequest.class);
        when(ctx.request()).thenReturn(sdkRequest);
        when(ctx.httpRequest()).thenReturn(request);
        return ctx;
    }
}
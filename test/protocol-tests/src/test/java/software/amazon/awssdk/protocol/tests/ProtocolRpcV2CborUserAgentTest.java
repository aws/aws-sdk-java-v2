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

package software.amazon.awssdk.protocol.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.useragent.BusinessMetricCollection.METRIC_SEARCH_PATTERN;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolsmithyrpcv2.ProtocolSmithyrpcv2AsyncClient;
import software.amazon.awssdk.services.protocolsmithyrpcv2.ProtocolSmithyrpcv2AsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;

class ProtocolRpcV2CborUserAgentTest {
    private CapturingInterceptor interceptor;

    private static final String USER_AGENT_HEADER_NAME = "User-Agent";
    private static final StaticCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    @BeforeEach
    public void setup() {
        this.interceptor = new CapturingInterceptor();
    }

    @Test
    void when_rpcV2CborProtocolIsUsed_correctMetricIsAdded() {
        ProtocolSmithyrpcv2AsyncClientBuilder clientBuilder = asyncClientBuilderForRpcV2Cbor();

        assertThatThrownBy(() -> clientBuilder.build().operationWithNoInputOrOutput(r -> {}).join())
            .hasMessageContaining("stop");

        String userAgent = assertAndGetUserAgentString();
        assertThat(userAgent).matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.PROTOCOL_RPC_V2_CBOR.value()));
    }

    @Test
    void when_nonRpcV2CborProtocolIsUsed_rpcV2CborMetricIsNotAdded() {
        ProtocolRestJsonAsyncClientBuilder clientBuilder = asyncClientBuilderForRestJson();

        assertThatThrownBy(() -> clientBuilder.build().allTypes(r -> {}).join())
            .hasMessageContaining("stop");

        String userAgent = assertAndGetUserAgentString();
        assertThat(userAgent).doesNotMatch(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.PROTOCOL_RPC_V2_CBOR.value()));
    }

    private String assertAndGetUserAgentString() {
        Map<String, List<String>> headers = interceptor.context.httpRequest().headers();
        assertThat(headers).containsKey(USER_AGENT_HEADER_NAME);
        return headers.get(USER_AGENT_HEADER_NAME).get(0);
    }

    private ProtocolSmithyrpcv2AsyncClientBuilder asyncClientBuilderForRpcV2Cbor() {
        return ProtocolSmithyrpcv2AsyncClient.builder()
                                             .region(Region.US_WEST_2)
                                             .credentialsProvider(CREDENTIALS_PROVIDER)
                                             .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor));
    }

    private ProtocolRestJsonAsyncClientBuilder asyncClientBuilderForRestJson() {
        return ProtocolRestJsonAsyncClient.builder()
                                          .region(Region.US_WEST_2)
                                          .credentialsProvider(CREDENTIALS_PROVIDER)
                                          .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor));
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("stop");
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }
    }
}

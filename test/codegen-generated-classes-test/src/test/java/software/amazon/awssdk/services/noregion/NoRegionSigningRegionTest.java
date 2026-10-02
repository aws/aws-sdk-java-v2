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

package software.amazon.awssdk.services.noregion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;

/**
 * Verifies that signing region is correctly resolved at request time for a service
 * without AWS_REGION in its endpoint params. The endpoint rules define signingRegion: "us-east-1"
 * in the auth scheme, which should be applied at request time regardless of the client region.
 */
public class NoRegionSigningRegionTest {

    @Test
    void signingRegion_resolvedFromEndpointRulesAtRequestTime() {
        SigningRegionCapturingInterceptor interceptor = new SigningRegionCapturingInterceptor();

        NoRegionClient client = NoRegionClient.builder()
            .region(Region.US_WEST_2) // Client configured with us-west-2
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("akid", "skid")))
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .endpointOverride(java.net.URI.create("https://noregionservice.us-east-1.amazonaws.com"))
            .build();

        try {
            client.someOperation(r -> r.stringMember("test"));
        } catch (SigningRegionCapturingInterceptor.CaptureCompletedException e) {
            // Expected — we throw after capturing
        }

        // The signing region should come from the endpoint rules auth scheme (us-east-1),
        // not the client region (us-west-2)
        assertThat(interceptor.signingRegion).isNotNull();
        assertThat(interceptor.signingRegion.id()).isEqualTo("us-east-1");

        client.close();
    }

    private static class SigningRegionCapturingInterceptor implements ExecutionInterceptor {
        private Region signingRegion;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            signingRegion = executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION);
            throw new CaptureCompletedException();
        }

        static class CaptureCompletedException extends RuntimeException {
        }
    }
}

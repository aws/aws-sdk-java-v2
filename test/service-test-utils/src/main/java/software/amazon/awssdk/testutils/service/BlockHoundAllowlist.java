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

package software.amazon.awssdk.testutils.service;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

/**
 * Implements {@link BlockHoundIntegration} to explicitly allow SDK calls that are known to be blocking. Some calls (with an
 * associated tracking issue) may wrongly block, but we allow-list them so that existing integration tests will continue to pass
 * and so that we preserve visibility on future regression detection.
 * <p>
 * https://github.com/reactor/BlockHound/blob/master/docs/custom_integrations.md
 */
public class BlockHoundAllowlist implements BlockHoundIntegration {
    @Override
    public void applyTo(BlockHound.Builder builder) {
        // https://github.com/aws/aws-sdk-java-v2/issues/2145
        builder.allowBlockingCallsInside(
            "software.amazon.awssdk.http.nio.netty.internal.BetterSimpleChannelPool",
            "close"
        );

        // https://github.com/aws/aws-sdk-java-v2/issues/2360
        builder.allowBlockingCallsInside(
            "software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider",
            "getToken"
        );
    }
}

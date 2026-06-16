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

package software.amazon.awssdk.core.crac;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test {@link SdkWarmUpProvider} that counts {@link #warmUp()} invocations, used to verify
 * {@link java.util.ServiceLoader} discovery.
 *
 * <p>
 * The class is public because {@link java.util.ServiceLoader} requires a public no-argument constructor.
 */
public class TestWarmUpProvider implements SdkWarmUpProvider {

    private final AtomicInteger warmUpInvocations = new AtomicInteger(0);

    @Override
    public void warmUp() {
        warmUpInvocations.incrementAndGet();
    }

    int warmUpInvocations() {
        return warmUpInvocations.get();
    }
}

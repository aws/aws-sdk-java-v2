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

import org.junit.BeforeClass;

import software.amazon.awssdk.core.util.IdempotentUtils;

/**
 * All protocol tests should extend this class to ensure that the idempotency generator is overridden before the
 * client class is loaded and the generator is cached, otherwise some tests in this suite can break.
 */
public class ProtocolTestBase {
    @BeforeClass
    public static void overrideIdempotencyTokenGenerator() {
        IdempotentUtils.setGenerator(() -> "00000000-0000-4000-8000-000000000000");
    }
}

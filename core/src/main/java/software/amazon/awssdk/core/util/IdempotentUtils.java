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

package software.amazon.awssdk.core.util;

import java.util.UUID;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;

/**
 * Utility class to manage idempotency token
 */
@SdkProtectedApi
public final class IdempotentUtils {

    private static Supplier<String> generator = () -> UUID.randomUUID().toString();

    private IdempotentUtils() {
    }

    /**
     * @deprecated By {@link #getGenerator()}
     */
    @Deprecated
    @SdkProtectedApi
    public static String resolveString(String token) {
        return token != null ? token : generator.get();
    }

    @SdkProtectedApi
    public static Supplier<String> getGenerator() {
        return generator;
    }


    @SdkTestInternalApi
    public static void setGenerator(Supplier<String> newGenerator) {
        generator = newGenerator;
    }
}

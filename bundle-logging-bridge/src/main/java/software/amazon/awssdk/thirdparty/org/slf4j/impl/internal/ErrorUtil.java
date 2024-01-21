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

package software.amazon.awssdk.thirdparty.org.slf4j.impl.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Utility for reporting errors.
 */
@SdkInternalApi
public final class ErrorUtil {
    private ErrorUtil() {
    }

    // CHECKSTYLE:OFF - Disable failure on usage of System.err since there is presumably no logging framework available if we
    // need to use this.
    public static void report(String msg) {
        System.err.println(msg);
    }
    // CHECKSTYLE:ON
}

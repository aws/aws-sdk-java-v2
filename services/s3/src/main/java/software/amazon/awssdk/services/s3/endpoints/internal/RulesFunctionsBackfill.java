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

package software.amazon.awssdk.services.s3.endpoints.internal;

public class RulesFunctionsBackfill {
    @SafeVarargs
    public static <T> T coalesce(T... args) {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException("coalesce requires at least two arguments");
        }

        for (T arg : args) {
            if (arg != null) {
                return arg;
            }
        }

        // All preceding arguments empty, return last argument (even if empty)
        return args[args.length - 1];
    }
}

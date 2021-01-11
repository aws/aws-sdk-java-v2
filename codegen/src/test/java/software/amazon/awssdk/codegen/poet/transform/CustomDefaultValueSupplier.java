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

package software.amazon.awssdk.codegen.poet.transform;

import java.util.function.Supplier;

public class CustomDefaultValueSupplier {

    /**
     * Value that indicates the current account.
     */
    private static final String VALUE = "BLAHBLAH";

    private static final Supplier<String> INSTANCE = () -> VALUE;

    private CustomDefaultValueSupplier() {
    }

    public static Supplier<String> getInstance() {
        return INSTANCE;
    }

}

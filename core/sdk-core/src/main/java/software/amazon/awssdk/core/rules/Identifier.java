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

package software.amazon.awssdk.core.rules;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Identifiers for variables declared within the rule engine, e.g. from an {@code assign} statement.
 */
@SdkInternalApi
public final class Identifier {
    private String name;

    public Identifier(String name) {
        this.name = name;
    }

    public static Identifier fromString(String id) {
        throw new UnsupportedOperationException();
    }

    public String asString() {
        return name;
    }

    public String toString() {
        return name;
    }
}

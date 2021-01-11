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

package software.amazon.awssdk.protocols.json.internal.dom;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Represents a simple scalar JSON value. This can either be a JSON string, JSON number, or JSON boolean. All values
 * are coerced into a string value {@link #value()}.
 */
@SdkInternalApi
public final class SdkScalarNode implements SdkJsonNode {

    private final String value;

    private SdkScalarNode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String asText() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SdkScalarNode that = (SdkScalarNode) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    /**
     * This does not preserve the type of the original node. For example a JSON number will be printed out
     * as a JSON string here. As such this should be used for debugging and tests only.
     */
    @Override
    public String toString() {
        return "\"" + value + "\"";
    }

    static SdkScalarNode create(String value) {
        return new SdkScalarNode(value);
    }
}

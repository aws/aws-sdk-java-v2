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

package software.amazon.awssdk.codegen.poet.common.model;

import static software.amazon.awssdk.util.StringUtils.isNullOrEmpty;

import java.util.stream.Stream;
import javax.annotation.Generated;

/**
 * Some comment on the class itself
 */
@Generated("software.amazon.awssdk:codegen")
public enum TestEnumClass {
    Available("available"),

    PermanentFailure("permanent-failure");

    private final String value;

    private TestEnumClass(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Use this in place of valueOf.
     *
     * @param value
     *        real value
     * @return TestEnumClass corresponding to the value
     */
    public static TestEnumClass fromValue(String value) {
        if (isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        }
        return Stream.of(TestEnumClass.values()).filter(e -> e.toString().equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot create enum from " + value + " value!"));
    }
}

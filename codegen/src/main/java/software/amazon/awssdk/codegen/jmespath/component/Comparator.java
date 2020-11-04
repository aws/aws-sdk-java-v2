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

package software.amazon.awssdk.codegen.jmespath.component;

/**
 * A comparator within a {@link ComparatorExpression}.
 */
public enum Comparator {
    LESS_THAN_OR_EQUAL("<="),
    EQUAL("=="),
    GREATER_THAN_OR_EQUAL(">="),
    NOT_EQUAL("!="),
    LESS_THAN("<"),
    GREATER_THAN(">");

    private final String tokenSymbol;

    Comparator(String tokenSymbol) {
        this.tokenSymbol = tokenSymbol;
    }

    public String tokenSymbol() {
        return tokenSymbol;
    }
}

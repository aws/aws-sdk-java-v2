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

package software.amazon.awssdk.codegen.jmespath.parser;

/**
 * A JMESPath parser, for generating a {@link ParseResult} from a string between two provided bounds.
 */
@FunctionalInterface
public interface Parser<T> {
    /**
     * Parse a JMESPath string between the start position (inclusive) and end position (exclusive).
     */
    ParseResult<T> parse(int startPosition, int endPosition);
}

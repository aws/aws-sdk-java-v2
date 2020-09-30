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

import java.util.OptionalInt;

/**
 * A slice expression allows you to select a contiguous subset of an array. A slice has a start, stop, and step value. The
 * general form of a slice is [start:stop:step], but each component is optional and can be omitted.
 *
 * https://jmespath.org/specification.html#slices
 */
public class SliceExpression {
    private final Integer start;
    private final Integer stop;
    private final Integer step;

    public SliceExpression(Integer start, Integer stop, Integer step) {
        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    public OptionalInt start() {
        return toOptional(start);
    }

    public OptionalInt stop() {
        return toOptional(stop);
    }

    public OptionalInt step() {
        return toOptional(step);
    }

    private OptionalInt toOptional(Integer n) {
        return n == null ? OptionalInt.empty() : OptionalInt.of(n);
    }
}

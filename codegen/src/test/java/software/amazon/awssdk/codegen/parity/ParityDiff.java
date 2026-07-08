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

package software.amazon.awssdk.codegen.parity;

import java.util.Objects;

/**
 * One structural difference between two {@code IntermediateModel}s at a given
 * JSON path.
 */
final class ParityDiff {

    enum Type {
        /** Present in C2J, absent in Smithy. */
        MISSING,
        /** Absent in C2J, present in Smithy. */
        ADDED,
        /** Present in both, values differ. */
        CHANGED,
        /** Present in both, JSON node types differ (e.g. string vs object). */
        TYPE_MISMATCH
    }

    private final String path;
    private final Type type;
    private final String c2jValue;
    private final String smithyValue;

    ParityDiff(String path, Type type, String c2jValue, String smithyValue) {
        this.path = path;
        this.type = type;
        this.c2jValue = c2jValue;
        this.smithyValue = smithyValue;
    }

    String path() {
        return path;
    }

    Type type() {
        return type;
    }

    String c2jValue() {
        return c2jValue;
    }

    String smithyValue() {
        return smithyValue;
    }

    @Override
    public String toString() {
        switch (type) {
            case MISSING:
                return String.format("MISSING %s: %s", path, c2jValue);
            case ADDED:
                return String.format("ADDED   %s: %s", path, smithyValue);
            case CHANGED:
                return String.format("CHANGED %s: c2j=%s, smithy=%s", path, c2jValue, smithyValue);
            case TYPE_MISMATCH:
                return String.format("TYPE    %s: c2j=%s, smithy=%s", path, c2jValue, smithyValue);
            default:
                throw new IllegalStateException("unknown diff type: " + type);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParityDiff)) {
            return false;
        }
        ParityDiff other = (ParityDiff) o;
        return Objects.equals(path, other.path)
            && type == other.type
            && Objects.equals(c2jValue, other.c2jValue)
            && Objects.equals(smithyValue, other.smithyValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, type, c2jValue, smithyValue);
    }
}

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

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Base class for the types of values computable by the {@link RuleEngine}.
 */
@SdkInternalApi
public abstract class Value {
    /**
     * A string value.
     */
    public static class Str extends Value {
        private Str(String value) {
        }
    }

    /**
     * An integer value.
     */
    public static class Int extends Value {
        private Int(int value) {
        }
    }

    /**
     * A boolean value.
     */
    public static class Bool extends Value {
        private Bool(boolean value) {
        }
    }

    /**
     * An array value.
     */
    public static class Array extends Value {
        private Array(List<Value> value) {
        }
    }

    /**
     * A record (map) value.
     */
    public static class Record extends Value {
        private Record(Map<Identifier, Value> value) {
        }
    }

    public static Str fromStr(String value) {
        return new Str(value);
    }

    public static Int fromInteger(int value) {
        return new Int(value);
    }

    public static Bool fromBool(boolean value) {
        return new Bool(value);
    }

    public static Record fromRecord(Map<Identifier, Value> value) {
        return new Record(value);
    }
}

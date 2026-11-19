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

package software.amazon.awssdk.codegen.poet.rules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a function from the endpoint rules set standard library.
 */
public final class RuleFunctionMirror {
    private final String name;
    private final String javaName;
    private final RuleType returns;
    private final RuleType containingType;
    private final Map<String, RuleType> arguments;

    RuleFunctionMirror(Builder builder) {
        this.name = builder.name;
        this.javaName = builder.javaName != null ? builder.javaName : builder.name;
        this.returns = Validate.paramNotNull(builder.returns, "returns");
        this.containingType = Validate.paramNotNull(builder.containingType, "containingType");
        this.arguments = Collections.unmodifiableMap(new LinkedHashMap<>(builder.arguments));
    }

    public static Builder builder(String name) {
        return new Builder()
            .name(name);
    }

    public String name() {
        return name;
    }

    public RuleType returns() {
        return returns;
    }

    public RuleType containingType() {
        return containingType;
    }

    public String javaName() {
        return javaName;
    }

    public Map<String, RuleType> arguments() {
        return arguments;
    }

    public boolean matches(List<RuleType> argv) {
        if (argv.size() != arguments.size()) {
            return false;
        }
        int idx = 0;
        for (RuleType expected : arguments.values()) {
            if (!expected.equals(argv.get(idx))) {
                return false;
            }
            idx++;
        }
        return true;
    }

    static class Builder {
        private final Map<String, RuleType> arguments = new LinkedHashMap<>();
        private String name;
        private String javaName;
        private RuleType returns;
        private RuleType containingType;

        Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder javaName(String javaName) {
            this.javaName = javaName;
            return this;
        }

        public Builder returns(RuleType type) {
            this.returns = type;
            return this;
        }

        public Builder containingType(RuleType containingType) {
            this.containingType = containingType;
            return this;
        }

        public Builder addArgument(String name, RuleType type) {
            this.arguments.put(name, type);
            return this;
        }

        public RuleFunctionMirror build() {
            return new RuleFunctionMirror(this);
        }
    }
}

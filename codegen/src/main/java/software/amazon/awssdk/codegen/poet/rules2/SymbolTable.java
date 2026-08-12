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

package software.amazon.awssdk.codegen.poet.rules2;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.utils.Validate;

public final class SymbolTable {
    private final Map<String, RuleType> params;
    private final Map<String, RuleType> locals;
    private final Set<String> regionParams;

    SymbolTable(Builder builder) {
        this.params = Collections.unmodifiableMap(new LinkedHashMap<>(builder.params));
        this.locals = Collections.unmodifiableMap(new LinkedHashMap<>(builder.locals));
        this.regionParams = Collections.unmodifiableSet(new HashSet<>(builder.regionParams));
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isParam(String name) {
        return params.containsKey(name);
    }

    public RuleType paramType(String name) {
        return params.get(name);
    }

    public boolean isLocal(String name) {
        return locals.containsKey(name);
    }

    public RuleType localType(String name) {
        return locals.get(name);
    }

    public Map<String, RuleType> locals() {
        return locals;
    }

    public Map<String, RuleType> params() {
        return params;
    }

    /**
     * Returns the set of parameter names that are Region-typed in Java (i.e., the Java getter returns {@code Region}
     * rather than {@code String}). The codegen needs to append {@code .id()} when accessing these params to convert
     * to the String value expected by the endpoint rules.
     */
    public Set<String> regionParams() {
        return regionParams;
    }

    /**
     * Returns true if the given parameter name is a Region-typed param that needs {@code .id()} appended.
     */
    public boolean isRegionParam(String name) {
        return regionParams.contains(name);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private final Map<String, RuleType> params = new LinkedHashMap<>();
        private final Map<String, RuleType> locals = new LinkedHashMap<>();
        private final Set<String> regionParams = new HashSet<>();

        public Builder() {
        }

        public Builder(SymbolTable table) {
            this.params.putAll(table.params);
            this.locals.putAll(table.locals);
            this.regionParams.addAll(table.regionParams);
        }

        public Builder putParam(String name, RuleType type) {
            params.put(Validate.paramNotNull(name, "name"), Validate.paramNotNull(type, "type"));
            return this;
        }

        public RuleType param(String name) {
            return params.get(name);
        }

        public Builder putLocal(String name, RuleType type) {
            locals.put(Validate.paramNotNull(name, "name"), Validate.paramNotNull(type, "type"));
            return this;
        }

        public RuleType local(String name) {
            return locals.get(name);
        }

        public Builder addRegionParam(String name) {
            regionParams.add(Validate.paramNotNull(name, "name"));
            return this;
        }

        public SymbolTable build() {
            return new SymbolTable(this);
        }
    }
}

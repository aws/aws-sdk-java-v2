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
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.utils.Validate;

public final class SymbolTable {
    private final Map<String, RuleType> params;
    private final Map<String, RuleType> locals;
    private final String regionParamName;

    SymbolTable(Builder builder) {
        this.params = Collections.unmodifiableMap(new LinkedHashMap<>(builder.params));
        this.locals = Collections.unmodifiableMap(new LinkedHashMap<>(builder.locals));
        this.regionParamName = builder.regionParamName;
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

    public String regionParamName() {
        return regionParamName;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private final Map<String, RuleType> params = new LinkedHashMap<>();
        private final Map<String, RuleType> locals = new LinkedHashMap<>();
        private String regionParamName;

        public Builder() {
        }

        public Builder(SymbolTable table) {
            this.params.putAll(table.params);
            this.locals.putAll(table.locals);
            this.regionParamName = table.regionParamName;
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

        public Builder regionParamName(String regionParamName) {
            this.regionParamName = regionParamName;
            return this;
        }

        public SymbolTable build() {
            return new SymbolTable(this);
        }
    }
}

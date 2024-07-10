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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.utils.Validate;

public class RuleType {
    private static final ClassName LIST = ClassName.get(List.class);
    private final String name;
    private final TypeName type;
    private final TypeName baseType;
    private final List<TypeName> typeParams;
    private final Map<String, RuleType> properties;
    private final RuleType ruleTypeParam;
    private final String className;
    private final String packageName;

    RuleType(Builder builder) {
        this.name = Validate.paramNotNull(builder.name, "name");
        this.baseType = builder.baseType;
        this.typeParams = Collections.unmodifiableList(new ArrayList<>(builder.typeParams));
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
        this.ruleTypeParam = builder.ruleTypeParam;
        this.className = builder.className;
        this.packageName = builder.packageName;
        if (builder.baseType == null) {
            this.type = javaType(ClassName.get(builder.packageName, builder.className), this.typeParams);
        } else {
            this.type = javaType(this.baseType, this.typeParams);
        }
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    static TypeName javaType(TypeName base, List<TypeName> params) {
        if (params.isEmpty()) {
            return base;
        }
        if (!(base instanceof ClassName)) {
            throw new IllegalArgumentException("Cannot cast base to ClassName");
        }
        TypeName[] paramTypes = params.toArray(new TypeName[0]);
        return ParameterizedTypeName.get((ClassName) base, paramTypes);
    }

    public TypeName javaType() {
        return type;
    }

    public List<TypeName> typeParams() {
        return typeParams;
    }

    public Map<String, RuleType> properties() {
        return properties;
    }


    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RuleType ruleType = (RuleType) o;

        if (!Objects.equals(name, ruleType.name)) {
            return false;
        }
        if (!Objects.equals(type, ruleType.type)) {
            return false;
        }
        if (!Objects.equals(baseType, ruleType.baseType)) {
            return false;
        }
        if (!Objects.equals(typeParams, ruleType.typeParams)) {
            return false;
        }
        if (!Objects.equals(properties, ruleType.properties)) {
            return false;
        }
        if (!Objects.equals(ruleTypeParam, ruleType.ruleTypeParam)) {
            return false;
        }
        if (!Objects.equals(className, ruleType.className)) {
            return false;
        }
        return Objects.equals(packageName, ruleType.packageName);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (baseType != null ? baseType.hashCode() : 0);
        result = 31 * result + (typeParams != null ? typeParams.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (ruleTypeParam != null ? ruleTypeParam.hashCode() : 0);
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
        return result;
    }

    public RuleType property(String name) {
        return properties.get(name);
    }

    public RuleType ruleTypeParam() {
        return ruleTypeParam;
    }

    public boolean isList() {
        return baseType.equals(LIST);
    }

    public RuleType typeParam() {
        return ruleTypeParam;
    }

    public String name() {
        return name;
    }

    public TypeName type() {
        return type;
    }

    public TypeName baseType() {
        return baseType;
    }

    public String className() {
        return className;
    }

    public String packageName() {
        return packageName;
    }

    public static class Builder {
        private final String name;
        private final List<TypeName> typeParams = new ArrayList<>();
        private final Map<String, RuleType> properties = new LinkedHashMap<>();
        private TypeName baseType;
        private RuleType ruleTypeParam;
        private String className;
        private String packageName;

        private Builder(String name) {
            this.name = name;
        }

        public Builder baseType(TypeName baseType) {
            this.baseType = baseType;
            return this;
        }

        public Builder addTypeParam(TypeName param) {
            this.typeParams.add(param);
            return this;
        }

        public Builder ruleTypeParam(RuleType type) {
            this.ruleTypeParam = type;
            return this;
        }

        public Builder putProperty(String name, RuleType type) {
            this.properties.put(name, type);
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public RuleType build() {
            return new RuleType(this);
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.endpoints.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public class Parameters {
    private final List<Parameter> parameters;

    private Parameters(Builder b) {
        this.parameters = b.parameters;
    }

    public List<Parameter> toList() {
        return parameters;
    }

    public Optional<Parameter> get(Identifier name) {
        return parameters.stream().filter((param) -> param.getName().equals(name)).findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Parameters that = (Parameters) o;

        return parameters != null ? parameters.equals(that.parameters) : that.parameters == null;
    }

    @Override
    public int hashCode() {
        return parameters != null ? parameters.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Parameters{" + "parameters=" + parameters + '}';
    }

    public static Parameters fromNode(JsonNode node) {
        Map<String, JsonNode> paramsObj = node.asObject();

        Builder b = builder();

        paramsObj.forEach((name, obj) -> {
            b.addParameter(Parameter.fromNode(name, obj));
        });

        return b.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Parameter> parameters = new ArrayList<>();

        public Builder addParameter(Parameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        public Parameters build() {
            return new Parameters(this);
        }
    }

}

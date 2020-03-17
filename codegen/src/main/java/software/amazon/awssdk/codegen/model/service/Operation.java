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

package software.amazon.awssdk.codegen.model.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import software.amazon.awssdk.codegen.model.intermediate.EndpointDiscovery;

public class Operation {

    private String name;

    private boolean deprecated;

    private Http http;

    private Input input;

    private Output output;

    private String documentation;

    private String authorizer;

    private List<ErrorMap> errors;

    private boolean requiresApiKey;

    @JsonProperty("endpointdiscovery")
    private EndpointDiscovery endpointDiscovery;

    @JsonProperty("endpointoperation")
    private boolean endpointOperation;

    private EndpointTrait endpoint;

    @JsonProperty("authtype")
    private AuthType authType = AuthType.IAM;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Operation withName(String name) {
        this.name = name;
        return this;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Http getHttp() {
        return http;
    }

    public void setHttp(Http http) {
        this.http = http;
    }

    public Operation withHttp(Http http) {
        this.http = http;
        return this;
    }

    public Input getInput() {
        return input;
    }

    public void setInput(Input input) {
        this.input = input;
    }

    public Operation withInput(Input input) {
        this.input = input;
        return this;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public List<ErrorMap> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorMap> errors) {
        this.errors = errors;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getAuthorizer() {
        return authorizer;
    }

    public void setAuthorizer(String authorizer) {
        this.authorizer = authorizer;
    }

    public boolean requiresApiKey() {
        return requiresApiKey;
    }

    public void setRequiresApiKey(boolean requiresApiKey) {
        this.requiresApiKey = requiresApiKey;
    }

    public EndpointDiscovery getEndpointDiscovery() {
        return endpointDiscovery;
    }

    public void setEndpointDiscovery(EndpointDiscovery endpointDiscovery) {
        this.endpointDiscovery = endpointDiscovery;
    }

    public boolean isEndpointOperation() {
        return endpointOperation;
    }

    public void setEndpointOperation(boolean endpointOperation) {
        this.endpointOperation = endpointOperation;
    }

    public EndpointTrait getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(EndpointTrait endpoint) {
        this.endpoint = endpoint;
    }
}

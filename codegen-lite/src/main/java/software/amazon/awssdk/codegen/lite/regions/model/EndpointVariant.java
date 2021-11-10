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

package software.amazon.awssdk.codegen.lite.regions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EndpointVariant {
    private String dnsSuffix;
    private String hostname;
    private List<String> tags;

    public String getDnsSuffix() {
        return dnsSuffix;
    }

    @JsonProperty(value = "dnsSuffix")
    public void setDnsSuffix(String dnsSuffix) {
        this.dnsSuffix = dnsSuffix;
    }

    public String getHostname() {
        return hostname;
    }

    @JsonProperty(value = "hostname")
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public List<String> getTags() {
        return tags;
    }

    @JsonProperty(value = "tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

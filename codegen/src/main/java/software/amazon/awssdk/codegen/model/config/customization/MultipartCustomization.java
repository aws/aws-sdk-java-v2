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

package software.amazon.awssdk.codegen.model.config.customization;

public class MultipartCustomization {
    private String multipartConfigurationClass;
    private String multipartConfigMethodDoc;
    private String multipartEnableMethodDoc;
    private String contextParamEnabledKey;
    private String contextParamConfigKey;

    public String getMultipartConfigurationClass() {
        return multipartConfigurationClass;
    }

    public void setMultipartConfigurationClass(String multipartConfigurationClass) {
        this.multipartConfigurationClass = multipartConfigurationClass;
    }

    public String getMultipartConfigMethodDoc() {
        return multipartConfigMethodDoc;
    }

    public void setMultipartConfigMethodDoc(String multipartMethodDoc) {
        this.multipartConfigMethodDoc = multipartMethodDoc;
    }

    public String getMultipartEnableMethodDoc() {
        return multipartEnableMethodDoc;
    }

    public void setMultipartEnableMethodDoc(String multipartEnableMethodDoc) {
        this.multipartEnableMethodDoc = multipartEnableMethodDoc;
    }

    public String getContextParamEnabledKey() {
        return contextParamEnabledKey;
    }

    public void setContextParamEnabledKey(String contextParamEnabledKey) {
        this.contextParamEnabledKey = contextParamEnabledKey;
    }

    public String getContextParamConfigKey() {
        return contextParamConfigKey;
    }

    public void setContextParamConfigKey(String contextParamConfigKey) {
        this.contextParamConfigKey = contextParamConfigKey;
    }
}

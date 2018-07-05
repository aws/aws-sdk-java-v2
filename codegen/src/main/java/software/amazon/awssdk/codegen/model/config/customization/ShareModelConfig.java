/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

/**
 * Contains custom properties for services that share models with other services.
 */
public class ShareModelConfig {

    /**
     * A service name that this service client should share models with. The models and non-request marshallers will be generated
     * into the same directory as the provided service's models.
     */
    private String shareModelWith;

    /**
     * The package name of the provided service. Service name will be used if not provided.
     */
    private String packageName;

    public String getShareModelWith() {
        return shareModelWith;
    }

    public void setShareModelWith(String shareModelWith) {
        this.shareModelWith = shareModelWith;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}

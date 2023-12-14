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

import software.amazon.awssdk.utils.Pair;

/**
 * Customization to change package name.
 */
public class CustomPackageName {

    /**
     * Change package root, by default it's "software.amazon.awssdk.services.[serviceId]"
     */
    private String rootPackageName;

    public String getRootPackageName() {
        return rootPackageName;
    }

    public void setRootPackageName(String rootPackageName) {
        this.rootPackageName = rootPackageName;
    }

    /**
     * Split the root package to [prefix].[suffix] pair. For example: "software.amazon.awssdk.services.s3" will be split into
     * "software.amazon.awssdk.services" and "s3".
     */
    public Pair<String, String> split() {
        if (rootPackageName == null) {
            return null;
        }
        int i = rootPackageName.lastIndexOf('.');
        return Pair.of(rootPackageName.substring(0, i), rootPackageName.substring(i + 1, rootPackageName.length()));
    }
}

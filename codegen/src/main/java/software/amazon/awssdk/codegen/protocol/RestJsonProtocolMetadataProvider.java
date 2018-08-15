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

package software.amazon.awssdk.codegen.protocol;

public class RestJsonProtocolMetadataProvider extends BaseJsonProtocolMetadataProvider {
    public static final long serialVersionUID = 1L;

    /**
     * For Rest-JSON, we used to set empty content type in V1. This is to support a single service having an issue.
     * See TT0059807265.
     *
     * Removing the customization in V2. If the service team still has issue, we can add customization for that specific service.
     */
    @Override
    public String getContentType() {
        return null;
    }
}

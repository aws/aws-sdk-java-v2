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

import software.amazon.awssdk.core.exception.SdkServiceException;

public class ApiGatewayProtocolMetadataProvider extends BaseJsonProtocolMetadataProvider {
    public static final long serialVersionUID = 1L;

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public String getProtocolFactoryImplFqcn() {
        return "software.amazon.awssdk.opensdk.protect.protocol.ApiGatewayProtocolFactoryImpl";
    }

    @Override
    public String getBaseExceptionFqcn() {
        return SdkServiceException.class.getName();
    }

    @Override
    public String getRequestBaseFqcn() {
        return "software.amazon.awssdk.opensdk.BaseRequest";
    }

}


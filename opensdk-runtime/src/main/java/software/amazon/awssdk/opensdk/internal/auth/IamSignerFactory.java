/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.opensdk.internal.auth;

import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.Signer;

public class IamSignerFactory {
    private final String region;

    public IamSignerFactory(String region) {
        this.region = region;
    }

    public Signer createSigner() {
        final Aws4Signer signer = new Aws4Signer();
        signer.setRegionName(region);
        // API Gateway always has the same signing name
        signer.setServiceName("execute-api");
        return signer;
    }
}

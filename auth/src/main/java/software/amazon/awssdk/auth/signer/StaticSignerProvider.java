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

package software.amazon.awssdk.auth.signer;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.runtime.auth.Signer;
import software.amazon.awssdk.core.runtime.auth.SignerProvider;
import software.amazon.awssdk.core.runtime.auth.SignerProviderContext;

/**
 * Implementation of {@link SignerProvider} that always returns the same signer regardless of
 * context.
 */
@SdkProtectedApi
public class StaticSignerProvider extends SignerProvider {

    private final Signer signer;

    private StaticSignerProvider(Signer signer) {
        this.signer = signer;
    }

    public static StaticSignerProvider create(Signer signer) {
        return new StaticSignerProvider(signer);
    }

    @Override
    public Signer getSigner(SignerProviderContext context) {
        return signer;
    }
}

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

// TODO: Figure out the right module/package for this. auth? http-auth?

package software.amazon.awssdk.core;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.Validate;

/**
 * A container for the identity resolver, signer and auth option that we selected for use with this service call attempt.
 */
@SdkProtectedApi
public final class SelectedAuthScheme<T extends Identity> {

    public static final String SMITHY_NO_AUTH = "smithy.api#noAuth";

    private CompletableFuture<? extends T> identity;
    private HttpSigner<T> signer;
    private AuthSchemeOption authSchemeOption;

    public SelectedAuthScheme(CompletableFuture<? extends T> identity,
                              HttpSigner<T> signer,
                              AuthSchemeOption authSchemeOption) {
        this.identity = identity;
        this.signer = signer;
        this.authSchemeOption = Validate.paramNotNull(authSchemeOption, "authSchemeOption");
    }

    public CompletableFuture<? extends T> identity() {
        return identity;
    }

    public HttpSigner<T> signer() {
        return signer;
    }

    public AuthSchemeOption authSchemeOption() {
        return authSchemeOption;
    }

    public boolean supportsSigning() {
        return !authSchemeOption.schemeId().equals(SMITHY_NO_AUTH);
    }
}

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

package software.amazon.awssdk.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A container for the identity resolver, signer and auth option that we selected for use with this service call attempt.
 */
@SdkProtectedApi
public final class SelectedAuthScheme<T extends Identity> {
    private final CompletableFuture<? extends T> identity;
    private final HttpSigner<T> signer;
    private final AuthSchemeOption authSchemeOption;
    private final String callingCode;

    public SelectedAuthScheme(CompletableFuture<? extends T> identity,
                              HttpSigner<T> signer,
                              AuthSchemeOption authSchemeOption) {
        this.callingCode = getCallingCode();
        this.identity = Validate.paramNotNull(identity, "identity");
        this.signer = Validate.paramNotNull(signer, "signer");
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

    private final String getCallingCode() {
        String result = "<unknown>";
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            result = sw.toString();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        authSchemeOption.forEachSignerProperty(new AuthSchemeOption.SignerPropertyConsumer() {
            @Override
            public <Object> void accept(SignerProperty<Object> propertyKey, Object propertyValue) {
                buf.append(propertyKey).append(": ").append(propertyValue).append(", ");

            }
        });
        String res = ToString.builder("SelectedAuthScheme")
                       .add("signerProperties", buf.toString())
                       .build();
        return res; // + "\nCreated at:\n" + callingCode + "\n================>> END";
    }
}

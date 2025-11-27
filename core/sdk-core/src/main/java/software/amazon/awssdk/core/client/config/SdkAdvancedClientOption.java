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

package software.amazon.awssdk.core.client.config;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;


/**
 * A collection of advanced options that can be configured on an AWS client via
 * {@link ClientOverrideConfiguration.Builder#putAdvancedOption(SdkAdvancedClientOption, Object)}.
 *
 * <p>These options are usually not required outside of testing or advanced libraries, so most users should not need to configure
 * them.</p>
 *
 * @param <T> The type of value associated with the option.
 */
@SdkPublicApi
public class SdkAdvancedClientOption<T> extends ClientOption<T> {
    private static final Set<SdkAdvancedClientOption<?>> OPTIONS = ConcurrentHashMap.newKeySet();

    /**
     * Set the prefix of the user agent that is sent with each request to AWS.
     */
    public static final SdkAdvancedClientOption<String> USER_AGENT_PREFIX = new SdkAdvancedClientOption<>(String.class);

    /**
     * Set the suffix of the user agent that is sent with each request to AWS.
     */
    public static final SdkAdvancedClientOption<String> USER_AGENT_SUFFIX = new SdkAdvancedClientOption<>(String.class);

    /**
     * Define the signer that should be used when authenticating with AWS.
     *
     * @deprecated Replaced by {@link HttpSigner}.
     * <p>
     * <b>Migration options:</b>
     * <ul>
     * <li>To customize signing logic: Configure via {@link SdkClientBuilder#putAuthScheme(AuthScheme)}.
     *     See {@link Signer} for examples.</li>
     * <li>To override signing properties only: Use custom {@link AuthSchemeProvider}.
     *     See {@link AuthSchemeProvider} for examples.</li>
     * </ul>
     */
    @Deprecated
    public static final SdkAdvancedClientOption<Signer> SIGNER = new SdkAdvancedClientOption<>(Signer.class);

    /**
     * Define the signer that should be used for token-based authentication with AWS.
     * @deprecated Replaced by {@link HttpSigner}.
     * <p>
     * <b>Migration options:</b>
     * <ul>
     * <li>To customize signing logic: Configure via {@link SdkClientBuilder#putAuthScheme(AuthScheme)}.
     *     See {@link Signer} for examples.</li>
     * <li>To override signing properties only: Use custom {@link AuthSchemeProvider}.
     *     See {@link AuthSchemeProvider} for examples.</li>
     * </ul>
     */
    public static final SdkAdvancedClientOption<Signer> TOKEN_SIGNER = new SdkAdvancedClientOption<>(Signer.class);

    /**
     * SDK uses endpoint trait and hostPrefix trait specified in service model to modify
     * the endpoint host that the API request is sent to.
     *
     * Customers can set this value to True to disable the behavior.
     */
    public static final SdkAdvancedClientOption<Boolean> DISABLE_HOST_PREFIX_INJECTION =
        new SdkAdvancedClientOption<>(Boolean.class);

    protected SdkAdvancedClientOption(Class<T> valueClass) {
        super(valueClass);
        OPTIONS.add(this);
    }

    /**
     * Retrieve all of the advanced client options loaded so far.
     */
    static Set<SdkAdvancedClientOption<?>> options() {
        return Collections.unmodifiableSet(OPTIONS);
    }
}

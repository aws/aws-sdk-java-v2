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

package software.amazon.awssdk.core.client.config;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.signer.Signer;


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
    /**
     * Set the prefix of the user agent that is sent with each request to AWS.
     */
    @ReviewBeforeRelease("This should either be changed when we refactor metrics, or the comment should be expanded upon.")
    public static final SdkAdvancedClientOption<String> USER_AGENT_PREFIX = new SdkAdvancedClientOption<>(String.class);

    /**
     * Set the suffix of the user agent that is sent with each request to AWS.
     */
    @ReviewBeforeRelease("This should either be changed when we refactor metrics, or the comment should be expanded upon.")
    public static final SdkAdvancedClientOption<String> USER_AGENT_SUFFIX = new SdkAdvancedClientOption<>(String.class);

    /**
     * Define the signer that should be used when authenticating with AWS.
     */
    public static final SdkAdvancedClientOption<Signer> SIGNER = new SdkAdvancedClientOption<>(Signer.class);

    protected SdkAdvancedClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}

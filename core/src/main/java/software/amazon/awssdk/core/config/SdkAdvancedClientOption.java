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

package software.amazon.awssdk.core.config;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.runtime.auth.SignerProvider;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * A collection of advanced options that can be configured on an AWS client via
 * {@link ClientOverrideConfiguration.Builder#advancedOption(SdkAdvancedClientOption, Object)}.
 *
 * <p>These options are usually not required outside of testing or advanced libraries, so most users should not need to configure
 * them.</p>
 *
 * @param <T> The type of value associated with the option.
 */
@ReviewBeforeRelease("Ensure that all of these options are actually advanced.")
public class SdkAdvancedClientOption<T> extends AttributeMap.Key<T> {
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
     * Configure the signer factory that should be used when generating signers in communication with AWS.
     */
    @ReviewBeforeRelease("This should either be changed when we refactor signers, or the comment should be expanded upon.")
    public static final SdkAdvancedClientOption<SignerProvider> SIGNER_PROVIDER = new SdkAdvancedClientOption<>(SignerProvider
                                                                                                                    .class);

    protected SdkAdvancedClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}

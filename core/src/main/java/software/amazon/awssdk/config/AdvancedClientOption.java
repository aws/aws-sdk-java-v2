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

package software.amazon.awssdk.config;

import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.client.builder.ClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * A collection of advanced options that can be configured on an AWS client via
 * {@link ClientOverrideConfiguration.Builder#advancedOption(AdvancedClientOption, Object)}.
 *
 * <p>These options are usually not required outside of testing or advanced libraries, so most users should not need to configure
 * them.</p>
 *
 * @param <T> The type of value associated with the option.
 */
@ReviewBeforeRelease("Ensure that all of these options are actually advanced.")
public class AdvancedClientOption<T> extends AttributeMap.Key<T> {
    /**
     * Set the prefix of the user agent that is sent with each request to AWS.
     */
    @ReviewBeforeRelease("This should either be changed when we refactor metrics, or the comment should be expanded upon.")
    public static final AdvancedClientOption<String> USER_AGENT_PREFIX = new AdvancedClientOption<>(String.class);

    /**
     * Set the suffix of the user agent that is sent with each request to AWS.
     */
    @ReviewBeforeRelease("This should either be changed when we refactor metrics, or the comment should be expanded upon.")
    public static final AdvancedClientOption<String> USER_AGENT_SUFFIX = new AdvancedClientOption<>(String.class);

    /**
     * Configure the signer factory that should be used when generating signers in communication with AWS.
     */
    @ReviewBeforeRelease("This should either be changed when we refactor signers, or the comment should be expanded upon.")
    public static final AdvancedClientOption<SignerProvider> SIGNER_PROVIDER = new AdvancedClientOption<>(SignerProvider.class);

    /**
     * AWS Region the client was configured with. Note that this is not always the signing region in the case of global
     * services like IAM.
     */
    public static final AdvancedClientOption<Region> AWS_REGION = new AdvancedClientOption<>(Region.class);

    /**
     * Whether region detection should be enabled. Region detection is used when the {@link ClientBuilder#region(Region)} is not
     * specified. This is enabled by default.
     */
    @ReviewBeforeRelease("This is AWS-specific and should probably be moved to a separate enum.")
    public static final AdvancedClientOption<Boolean> ENABLE_DEFAULT_REGION_DETECTION =
            new AdvancedClientOption<>(Boolean.class);

    protected AdvancedClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}

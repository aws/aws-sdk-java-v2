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

import java.net.URI;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An interface that represents all configuration required by an AWS client in order to operate. AWS clients accept
 * implementations of the child interfaces ({@link SdkAsyncClientConfiguration} or {@link SdkSyncClientConfiguration}) when
 *  * constructed.
 *
 * <p>Implementations of this interface are not necessarily immutable or thread safe. If thread safety is required, consider
 * creating an immutable representation with {@link SdkImmutableClientConfiguration}.</p>
 */
@SdkInternalApi
@ReviewBeforeRelease("Do we want to have all optional Client*Configuration objects merged under one 'ClientOverrideConfig', to "
                     + "make it easier to find the required configuration, like endpoint? This would also make it clear why "
                     + "the credential configuration is separated from the other security configuration.")
public interface SdkClientConfiguration {
    /**
     * Override default client configuration options, such as request timeouts, retry behavior and compression. This will never
     * return null.
     */
    ClientOverrideConfiguration overrideConfiguration();

    /**
     * The endpoint with which the SDK should communicate.
     */
    URI endpoint();
}

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

package software.amazon.awssdk.auth.credentials.internal;

import java.lang.reflect.InvocationTargetException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenCredentialsProviderFactory;

/**
 * Utility class used to configure credential providers based on JWT web identity tokens.
 */
@SdkInternalApi
public final class WebIdentityCredentialsUtils {

    private static final String STS_WEB_IDENTITY_CREDENTIALS_PROVIDER_FACTORY =
        "software.amazon.awssdk.services.sts.internal.StsWebIdentityCredentialsProviderFactory";

    private WebIdentityCredentialsUtils() {
    }

    /**
     * Resolves the StsWebIdentityCredentialsProviderFactory from the Sts module if on the classpath to allow
     * JWT web identity tokens to be used as credentials.
     *
     * @return WebIdentityTokenCredentialsProviderFactory
     */
    public static WebIdentityTokenCredentialsProviderFactory factory() {
        try {
            Class<?> stsCredentialsProviderFactory = Class.forName(STS_WEB_IDENTITY_CREDENTIALS_PROVIDER_FACTORY, true,
                                                                   Thread.currentThread().getContextClassLoader());
            return (WebIdentityTokenCredentialsProviderFactory) stsCredentialsProviderFactory.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To use web identity tokens, the 'sts' service module must be on the class path.", e);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create a web identity token credentials provider.", e);
        }
    }
}

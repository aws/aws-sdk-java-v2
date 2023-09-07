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

package software.amazon.awssdk.http.auth.aws.internal.util;

import java.time.Clock;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.crt.internal.signer.DefaultAwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.eventstream.internal.signer.EventStreamV4PayloadSigner;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.ClassLoaderHelper;
import software.amazon.awssdk.utils.Logger;

/**
 * Utilities for loading of classes and objects which have optional dependencies, and therefore need to be safely checked at
 * runtime in order to use.
 */
@SdkInternalApi
public final class LoaderUtil {
    private static final Logger LOG = Logger.loggerFor(LoaderUtil.class);

    private static final String HTTP_AUTH_AWS_CRT_PATH =
        "software.amazon.awssdk.http.auth.aws.crt.HttpAuthAwsCrt";
    private static final String HTTP_AUTH_AWS_CRT_MODULE = "http-auth-aws-crt";
    private static final String HTTP_AUTH_AWS_EVENT_STREAM_PATH =
        "software.amazon.awssdk.http.auth.aws.eventstream.HttpAuthAwsEventStream";
    private static final String HTTP_AUTH_AWS_EVENT_STREAM_MODULE = "http-auth-aws-event-stream";

    private LoaderUtil() {
    }

    /**
     * A helpful method that checks that some class is available on the class-path. If it fails to load, it will throw an
     * exception based on why it failed to load. This should be used in cases where certain dependencies are optional, but the
     * dependency is used at compile-time for strong typing (i.e. {@link EventStreamV4PayloadSigner}).
     */
    public static void requireClass(String classPath, String module) {
        try {
            ClassLoaderHelper.loadClass(classPath, false);
        } catch (ClassNotFoundException e) {
            LOG.debug(() -> "Cannot find the " + classPath + " class: ", e);
            String msg = String.format(
                "Could not load class (%s). You must add a dependency on the '%s' module to enable this functionality: ",
                classPath,
                module
            );

            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not load class (%s): ", classPath), e);
        }
    }

    public static AwsV4aHttpSigner getAwsV4aHttpSigner() {
        requireClass(HTTP_AUTH_AWS_CRT_PATH, HTTP_AUTH_AWS_CRT_MODULE);
        return new DefaultAwsCrtV4aHttpSigner();
    }

    public static EventStreamV4PayloadSigner getEventStreamV4PayloadSigner(
        AwsCredentialsIdentity credentials,
        CredentialScope credentialScope,
        Clock signingClock) {

        requireClass(HTTP_AUTH_AWS_EVENT_STREAM_PATH, HTTP_AUTH_AWS_EVENT_STREAM_MODULE);
        return EventStreamV4PayloadSigner.builder()
                                         .credentials(credentials)
                                         .credentialScope(credentialScope)
                                         .signingClock(signingClock)
                                         .build();
    }
}

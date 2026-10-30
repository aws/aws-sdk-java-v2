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

package software.amazon.awssdk.s3.wiretap.client;

import java.util.List;
import software.amazon.awssdk.s3.wiretap.client.feature.Feature;

public final class ClientBuilds {

    private static final String CRT_RUNTIME_EXCEPTION = "CrtRuntimeException";

    private ClientBuilds() {
    }

    public static ClientSuccessOrFailure buildEither(ClientConfig config, ClientBuilder builder) {
        List<Feature<?>> inapplicable = config.inapplicableFeatures();
        if (!inapplicable.isEmpty()) {
            return ClientSuccessOrFailure.notApplicable(config, inapplicable);
        }
        try {
            return ClientSuccessOrFailure.success(config.buildSync(builder), config);
        } catch (RuntimeException e) {
            rethrowIfEnvironmentFailure(config, e);
            return ClientSuccessOrFailure.failure(new ClientBuildFailed(config, e));
        }
    }

    public static void rethrowIfEnvironmentFailure(ClientConfig config, RuntimeException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof UnsatisfiedLinkError
                    || cause instanceof NoClassDefFoundError
                    || CRT_RUNTIME_EXCEPTION.equals(cause.getClass().getSimpleName())) {
                throw new IllegalStateException(
                        "environment cannot build client config [" + config.describe() + "]: a native "
                                + "library failed to load, which is NOT an invalid feature combination "
                                + "and must not be recorded as one. Fix the runtime (see the cause) "
                                + "rather than the test: " + cause, e);
            }
            Throwable next = cause.getCause();
            cause = next == cause ? null : next;
        }
    }
}

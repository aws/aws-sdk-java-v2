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

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class ClientBuildFailed {

    @Getter
    private final ClientConfig config;
    @Getter
    private final Throwable failure;

    public ClientBuildFailed(ClientConfig config, Throwable failure) {
        this.config = config;
        this.failure = failure;
    }

    public String configLabel() {
        return config.describe();
    }

    public String message() {
        return failure.getMessage();
    }

    public boolean isInstanceOf(Class<? extends Throwable> type) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    public boolean messageContains(String substring) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(substring)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ClientBuildFailed[" + config.describe() + ": " + failure + "]";
    }
}

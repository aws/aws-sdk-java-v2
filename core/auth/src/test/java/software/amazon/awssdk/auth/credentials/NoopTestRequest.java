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

package software.amazon.awssdk.auth.credentials;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;

public class NoopTestRequest extends SdkRequest {
    private NoopTestRequest() {

    }

    @Override
    public Optional<? extends RequestOverrideConfiguration> overrideConfiguration() {
        return Optional.empty();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return Collections.emptyList();
    }

    public interface Builder extends SdkRequest.Builder {
        @Override
        NoopTestRequest build();
    }

    private static class BuilderImpl implements SdkRequest.Builder, Builder {

        @Override
        public RequestOverrideConfiguration overrideConfiguration() {
            return null;
        }

        @Override
        public NoopTestRequest build() {
            return new NoopTestRequest();
        }
    }
}

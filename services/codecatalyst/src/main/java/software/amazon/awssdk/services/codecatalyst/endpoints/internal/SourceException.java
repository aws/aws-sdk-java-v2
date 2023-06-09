/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst.endpoints.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkException;

@SdkInternalApi
public class SourceException extends SdkException {
    private SourceException(Builder b) {
        super(b);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    interface Builder extends SdkException.Builder {
        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder message(String message);

        @Override
        SourceException build();
    }

    public static class BuilderImpl extends SdkException.BuilderImpl implements Builder {
        @Override
        public Builder cause(Throwable cause) {
            super.cause(cause);
            return this;
        }

        @Override
        public Builder message(String message) {
            super.message(message);
            return this;
        }

        @Override
        public Builder writableStackTrace(Boolean writableStackTrace) {
            super.writableStackTrace(writableStackTrace);
            return this;
        }

        @Override
        public SourceException build() {
            return new SourceException(this);
        }
    }
}

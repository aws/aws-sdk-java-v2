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

package software.amazon.awssdk.services.acm.endpoints.internal;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkException;

@SdkInternalApi
public class RuleError extends SdkException {

    protected RuleError(BuilderImpl builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkException.Builder {
        @Override
        RuleError build();
    }

    public static <T> T ctx(String message, Supplier<T> f) {
        try {
            return f.get();
        } catch (Exception e) {
            throw builder().message(message).cause(e).build();
        }
    }

    public static <T> T ctx(String message, Runnable f) {
        return ctx(message, () -> {
            f.run();
            return null;
        });
    }

    private static class BuilderImpl extends SdkException.BuilderImpl implements Builder {
        @Override
        public RuleError build() {
            return new RuleError(this);
        }
    }
}

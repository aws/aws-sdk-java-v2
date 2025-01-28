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

package software.amazon.awssdk.core.exception;

import java.util.StringJoiner;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class SdkDiagnostics {
    private final Integer numAttempts;

    private SdkDiagnostics(Builder builder) {
        this.numAttempts = builder.numAttempts();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public String toString() {
        StringJoiner details = new StringJoiner(", ", "(", ")");
        details.add("SDK Diagnostics: numAttempts = " + numAttempts);
        return details.toString();
    }

    public SdkDiagnostics.Builder toBuilder() {
        return new SdkDiagnostics.BuilderImpl();
    }

    public interface Builder {
        /**
         * Sets the number of attempts.
         *
         * @param numAttempts The number of attempts
         * @return Returns the builder for method chaining
         */
        Builder numAttempts(Integer numAttempts);

        /**
         * Builds the SdkDiagnostics instance.
         *
         * @return A new SdkDiagnostics instance
         */
        SdkDiagnostics build();

        /**
         * Gets the number of attempts.
         *
         * @return Returns the attempt count
         */
        Integer numAttempts();
    }

    private static class BuilderImpl implements Builder {
        private Integer numAttempts;

        @Override
        public Builder numAttempts(Integer numAttempts) {
            this.numAttempts = numAttempts;
            return this;
        }

        @Override
        public Integer numAttempts() {
            return this.numAttempts;
        }

        @Override
        public SdkDiagnostics build() {
            return new SdkDiagnostics(this);
        }
    }
}
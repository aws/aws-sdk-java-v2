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

package software.amazon.awssdk.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;

@SdkPublicApi
public final class WarmUpConfiguration {
    private final boolean loadClasses;
    private final boolean initializeClasses;
    private final List<Runnable> primeFunctions;

    private WarmUpConfiguration(Builder builder) {
        this.loadClasses = builder.loadClasses == null || builder.loadClasses;
        this.initializeClasses = builder.initializeClasses == null || builder.initializeClasses;
        this.primeFunctions =
            Collections.unmodifiableList(builder.primeFunctions == null ? new ArrayList<>() : builder.primeFunctions);
    }

    public static WarmUpConfiguration.Builder builder() {
        return new WarmUpConfiguration.Builder();
    }

    /**
     * @return
     */
    public boolean preloadClasses() {
        return loadClasses;
    }

    /**
     * @return
     */
    public boolean initializeClasses() {
        return initializeClasses;
    }

    /**
     * @return
     */
    public List<Runnable> primeFunctions() {
        return primeFunctions;
    }

    public static final class Builder {
        private Boolean loadClasses;
        private Boolean initializeClasses;
        private List<Runnable> primeFunctions;

        /**
         * @return
         */
        public boolean preloadClasses() {
            return loadClasses;
        }

        public Builder preloadClasses(Boolean loadClasses) {
            this.loadClasses = loadClasses;
            return this;
        }

        /**
         * @return
         */
        public boolean initializeClasses() {
            return initializeClasses;
        }

        public Builder initializeClasses(Boolean initializeClasses) {
            this.initializeClasses = initializeClasses;
            return this;
        }

        public List<Runnable> primeFunctions() {
            return primeFunctions;
        }

        public Builder primeFunctions(List<Runnable> primeFunctions) {
            this.primeFunctions = primeFunctions;
            return this;
        }

        public Builder primeFunctions(Runnable... primeFunctions) {
            this.primeFunctions = Arrays.asList(primeFunctions);
            return this;
        }

        public WarmUpConfiguration build() {
            return new WarmUpConfiguration(this);
        }
    }
}

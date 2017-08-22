/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.interceptor;

import software.amazon.awssdk.handlers.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.utils.Validate;

/**
 * Defines the priority in which {@link ExecutionInterceptor}s should be executed. Interceptors are sorted by their priorities in
 * ascending order.
 *
 * <p>Most interceptors that aren't defined by special libraries or the service itself should use the {@link #USER} priority, or a
 * function of the {@link #USER} priority (eg. {@code new Priority(Priority.USER.value() - 1)}).</p>
 *
 * @see ExecutionInterceptor
 */
public class Priority implements Comparable<Priority> {
    /**
     * The default priority for an interceptor that is written for a specific service, without which the request will fail.
     *
     * <p>These interceptors are usually defined by the service that will be receiving the request in order to augment the request
     * with additional information or to modify the request because its JSON definition is insufficient. These are usually loaded
     * automatically using service-specific interceptors paths via
     * {@link ClasspathInterceptorChainFactory#getInterceptors(String)}.</p>
     */
    public static final Priority SERVICE = new Priority(1000);

    /**
     * The default priority for an interceptor that is written to be used across services.
     *
     * <p>These interceptors are usually defined by a library that will be reading the request for logging, metrics or auditing
     * purposes. These interceptors are usually defined by libraries that are automatically loaded from the classpath via
     * {@link ClasspathInterceptorChainFactory#getGlobalInterceptors()}.</p>
     */
    public static final Priority GLOBAL = new Priority(2000);

    /**
     * The default priority for an interceptor that is written by a consumer of the SDK.
     *
     * <p>These interceptors are usually registered directly into a specific client via
     * {@link software.amazon.awssdk.config.ClientOverrideConfiguration.Builder#addExecutionInterceptor(ExecutionInterceptor)}.
     * </p>
     */
    public static final Priority USER = new Priority(3000);

    private Integer value;

    /**
     * Create a priority with the provided value. This should usually not be used except in relation to another static priorities
     * (like {@link Priority#USER}).
     *
     * <p>
     * For example:
     * <ol>
     *     <li>{@code Priority.USER} should be used instead of {@code new Priority(3000)}</li>
     *     <li>{@code new Priority(Priority.USER.value() - 1)} should be used to specify an interceptor that happens before other
     *     {@code Priority.USER} interceptors</li>
     *     <li>{@code new Priority(Priority.USER.value() + 1)} should be used to specify an interceptor that happens after other
     *     {@code Priority.USER} interceptors</li>
     * </ol>
     * </p>
     */
    public Priority(Integer value) {
        this.value = Validate.paramNotNull(value, "value");
        Validate.isTrue(value >= 0, "Priority must be positive.");
    }

    @Override
    public int compareTo(Priority o) {
        return value.compareTo(o.value);
    }
}

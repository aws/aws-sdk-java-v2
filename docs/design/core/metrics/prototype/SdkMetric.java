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

/**
 * A specific SDK metric.
 *
 * @param <T> The type for values of this metric.
 */
@SdkPublicApi
public interface SdkMetric<T> {

    /**
     * @return The name of this metric.
     */
    public String name();

    /**
     * @return The categories of this metric.
     */
    public Set<MetricCategory> categories();

    /**
     * @return The level of this metric.
     */
    MetricLevel level();

    /**
     * @return The class of the value associated with this metric.
     */
    public Class<T> valueClass();

    /**
     * Cast the given object to the value class associated with this event.
     *
     * @param o The object.
     * @return The cast object.
     * @throws ClassCastException If {@code o} is not an instance of type {@code
     * T}.
     */
    public T convertValue(Object o);
}

/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.signerspi;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;

public class SignerContext {

    private final Map<ExecutionAttribute<?>, Object> attributes = new HashMap<>();

    /**
     * Retrieve the current value of the provided attribute in this collection of attributes. This will return null if the value
     * is not set.
     */
    @SuppressWarnings("unchecked") // Cast is safe due to implementation of {@link #putAttribute}
    public <U> U getAttribute(ExecutionAttribute<U> attribute) {
        return (U) attributes.get(attribute);
    }

    /**
     * Update or set the provided attribute in this collection of attributes.
     */
    public <U> SignerContext putAttribute(ExecutionAttribute<U> attribute, U value) {
        this.attributes.put(attribute, value);
        return this;
    }
}
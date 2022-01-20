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

package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * An attribute attached to a particular HTTP request execution, stored in {@link SdkHttpExecutionAttributes}. It can be
 * configured on an {@link AsyncExecuteRequest} via
 * {@link AsyncExecuteRequest.Builder#putHttpExecutionAttribute(SdkHttpExecutionAttribute,
 * Object)}
 *
 * @param <T> The type of data associated with this attribute.
 */
@SdkPublicApi
public abstract class SdkHttpExecutionAttribute<T> extends AttributeMap.Key<T> {

    protected SdkHttpExecutionAttribute(Class<T> valueType) {
        super(valueType);
    }

    protected SdkHttpExecutionAttribute(UnsafeValueType unsafeValueType) {
        super(unsafeValueType);
    }
}

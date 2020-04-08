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

package software.amazon.awssdk.protocols.query.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.protocols.core.Marshaller;

/**
 * Interface for marshallers for AWS/Query protocol.
 *
 * @param <T> Type being marshalled.
 */
@FunctionalInterface
@SdkInternalApi
public interface QueryMarshaller<T> extends Marshaller<T> {

    void marshall(QueryMarshallerContext context, String path, T val, SdkField<T> sdkField);
}

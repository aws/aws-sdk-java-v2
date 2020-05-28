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

package software.amazon.awssdk.protocols.json.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;

/**
 * AWS services expect an empty body when the payload member is null instead of an explicit JSON null.
 */
@SdkInternalApi
public class NullAsEmptyBodyProtocolRequestMarshaller implements ProtocolMarshaller<SdkHttpFullRequest> {

    private final ProtocolMarshaller<SdkHttpFullRequest> delegate;

    public NullAsEmptyBodyProtocolRequestMarshaller(ProtocolMarshaller<SdkHttpFullRequest> delegate) {
        this.delegate = delegate;
    }

    @Override
    public SdkHttpFullRequest marshall(SdkPojo pojo) {
        return delegate.marshall(pojo);
    }
}

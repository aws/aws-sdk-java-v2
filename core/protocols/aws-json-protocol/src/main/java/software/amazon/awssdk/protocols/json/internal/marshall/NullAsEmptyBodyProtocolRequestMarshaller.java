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

package software.amazon.awssdk.protocols.json.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;

/**
 * AWS services expect an empty body when the payload member is null instead of an explicit JSON null.
 * This implementation can be removed once CR-6541513 has been deployed to all services that use the payload trait.
 *
 * @param <OrigRequestT> Type of the original request object.
 */
@SdkInternalApi
public class NullAsEmptyBodyProtocolRequestMarshaller<OrigRequestT> implements ProtocolMarshaller<Request<OrigRequestT>> {

    private final ProtocolMarshaller<Request<OrigRequestT>> delegate;

    public NullAsEmptyBodyProtocolRequestMarshaller(ProtocolMarshaller<Request<OrigRequestT>> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Request<OrigRequestT> marshall(SdkPojo pojo) {
        return delegate.marshall(pojo);
    }
}

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

package software.amazon.awssdk.core.protocol;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.Request;

/**
 * Interface used by generated marshallers to transform a Java POJO in a {@link Request} object which represents an HTTP request.
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * {@code
 * ProtocolRequestMarshaller<FooRequest> = createProtocolMarshaller(...);
 * protocolMarshaller.startMarshalling();
 * protocolMarshaller.marshall(obj, marshallingInfo);
 * Request<FooRequest> marshalledRequest = protocolMarshaller.finishMarshalling();
 * }
 * </pre>
 *
 * @param <OrigRequestT> Type of the original request object.
 */
@SdkProtectedApi
// todo collapse
public interface ProtocolRequestMarshaller<OrigRequestT> extends ProtocolMarshaller<Request<OrigRequestT>> {

}

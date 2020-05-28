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

package software.amazon.awssdk.http.async;

import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A {@link Publisher} of HTTP content data that allows streaming operations for asynchronous HTTP clients.
 */
@SdkPublicApi
public interface SdkHttpContentPublisher extends Publisher<ByteBuffer> {

    /**
     * @return The content length of the data being produced.
     */
    Optional<Long> contentLength();

}

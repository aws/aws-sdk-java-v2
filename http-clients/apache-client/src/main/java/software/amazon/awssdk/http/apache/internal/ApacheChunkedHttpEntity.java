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

package software.amazon.awssdk.http.apache.internal;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Implementation of Apache {@link HttpEntityWrapper} that sets {@code isChunked()} to true.
 */
@SdkInternalApi
public class ApacheChunkedHttpEntity extends HttpEntityWrapper {

    /**
     * Creates a new entity wrapper that sets isChunked() to true.
     *
     * @param wrappedEntity the entity to wrap.
     */
    public ApacheChunkedHttpEntity(HttpEntity wrappedEntity) {
        super(wrappedEntity);
    }

    @Override
    public boolean isChunked() {
        return true;
    }
}

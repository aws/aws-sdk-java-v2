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

package software.amazon.awssdk.core.internal.handler;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * This class is needed as an alternative to {@link RequestBody#fromContentProvider(ContentStreamProvider, long, String)},
 * which buffers the entire content.
 *
 * <p>
 * THIS IS AN INTERNAL API. DO NOT USE IT OUTSIDE THE AWS SDK FOR JAVA V2.
 */
@SdkInternalApi
class SdkInternalOnlyRequestBody extends RequestBody {

    protected SdkInternalOnlyRequestBody(ContentStreamProvider contentStreamProvider, Long contentLength, String contentType) {
        super(contentStreamProvider, contentLength, contentType);
    }
}

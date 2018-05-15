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

package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Generic interface to take a representation of an HTTP request, make the HTTP call, and return a representation of an
 * HTTP response.
 *
 * <p>Implementations MUST be thread safe.</p>
 *
 * <p><b><i>Note: This interface will change between SDK versions and should not be implemented by SDK users.</i></b></p>
 */
@SdkInternalApi
public interface SdkHttpClient extends SdkAutoCloseable, ConfigurationProvider {

    /**
     * Create a {@link AbortableCallable} that can be used to execute the HTTP request.
     *
     * @param request        Representation of an HTTP request.
     * @param requestContext Contains any extra dependencies needed.
     * @return Task that can execute an HTTP request and can be aborted.
     */
    AbortableCallable<SdkHttpFullResponse> prepareRequest(SdkHttpFullRequest request, SdkRequestContext requestContext);

}

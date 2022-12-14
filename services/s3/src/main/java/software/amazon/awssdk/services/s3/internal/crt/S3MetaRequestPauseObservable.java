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

package software.amazon.awssdk.services.s3.internal.crt;

import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.crt.s3.S3MetaRequest;

/**
 * An observable that notifies the observer {@link S3CrtAsyncHttpClient} to pause the request.
 */
@SdkInternalApi
public class S3MetaRequestPauseObservable {

    private final Function<S3MetaRequest, ResumeToken> pause;
    private volatile S3MetaRequest request;

    public S3MetaRequestPauseObservable() {
        this.pause = S3MetaRequest::pause;
    }

    /**
     * Subscribe {@link S3MetaRequest} to be potentially paused later.
     */
    public  void subscribe(S3MetaRequest request) {
        this.request = request;
    }

    /**
     * Pause the request
     */
    public ResumeToken pause() {
        return pause.apply(request);
    }
}


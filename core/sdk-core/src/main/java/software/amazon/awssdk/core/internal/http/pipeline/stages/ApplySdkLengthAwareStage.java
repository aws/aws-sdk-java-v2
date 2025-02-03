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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.io.SdkLengthAwareInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public final class ApplySdkLengthAwareStage implements MutableRequestToRequestPipeline {

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder input,
                                              RequestExecutionContext context) throws Exception {

        if (input.contentStreamProvider() == null) {
            return input;
        }

        Optional<String> contentLengthOptional = input.firstMatchingHeader(Header.CONTENT_LENGTH);
        ContentStreamProvider contentStreamProvider = input.contentStreamProvider();

        assert contentStreamProvider != null;

        if (contentLengthOptional.isPresent()) {
            long contentLength = Long.parseLong(contentLengthOptional.get());

            ContentStreamProvider streamProvider = () -> new SdkLengthAwareInputStream(contentStreamProvider.newStream(),
                                                                                       contentLength);
            input.contentStreamProvider(streamProvider);
        }

        return input;
    }
}

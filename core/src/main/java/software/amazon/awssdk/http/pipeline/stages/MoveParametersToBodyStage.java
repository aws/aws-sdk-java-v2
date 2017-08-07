/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.pipeline.stages;

import static java.util.Collections.singletonList;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.handlers.AwsExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.util.SdkHttpUtils;

@ReviewBeforeRelease("Might only need to do this for certain protocols - ie query?")
// TODO how is this going to work with streaming input posts in asyncland
public final class MoveParametersToBodyStage implements MutableRequestToRequestPipeline {
    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder input, RequestExecutionContext context) {
        if (shouldPutParamsInBody(input, context)) {
            return putParams(input);
        }
        return input;
    }

    private boolean shouldPutParamsInBody(SdkHttpFullRequest.Builder input,
                                          RequestExecutionContext context) {
        return notSimpleDb(context) &&
               input.getHttpMethod() == SdkHttpMethod.POST &&
               input.getContent() == null &&
               input.getParameters() != null &&
               input.getParameters().size() > 0;
    }

    // TODO FIXME hacky hack
    @ReviewBeforeRelease("SigV2 expects query params to be signed as query params despite being in the body. Moving" +
                         " before signing breaks SimpleDB which still uses SigV2. Probably the best thing to do is" +
                         " make the SigV2 signer be aware that params are being moved into the body and move them back out" +
                         " and unencode and sign them as query params. We did a similiar thing in the V4 signer in 1.11.x" +
                         " but I'd rather have the grossness in the legacy signer implementation")
    private boolean notSimpleDb(RequestExecutionContext context) {
        return !"SimpleDBClient".equals(context.executionAttributes().getAttribute(AwsExecutionAttributes.SERVICE_NAME));
    }

    private SdkHttpFullRequest.Builder putParams(SdkHttpFullRequest.Builder input) {
        byte[] params = SdkHttpUtils.encodeParameters(input).getBytes(StandardCharsets.UTF_8);

        return input.clearQueryParameters()
                    .content(new ByteArrayInputStream(params))
                    .header("Content-Length", singletonList(String.valueOf(params.length)))
                    .header("Content-Type", singletonList("application/x-www-form-urlencoded; charset=" +
                                                          lowerCase(StandardCharsets.UTF_8.toString())));
    }
}

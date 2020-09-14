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

package software.amazon.awssdk.authcrt.signer;

import java.time.Clock;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.authcrt.signer.internal.BaseCrtAws4aSigner;
import software.amazon.awssdk.authcrt.signer.params.Aws4aPresignerParams;
import software.amazon.awssdk.authcrt.signer.params.Aws4aSignerParams;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;

/**
 * Signer implementation that signs requests with the asymmetric AWS4 (aws4a) signing protocol.
 */
@SdkPublicApi
public final class Aws4aSigner extends BaseCrtAws4aSigner<Aws4aSignerParams, Aws4aPresignerParams> {

    public static final ExecutionAttribute<Clock> SIGNING_CLOCK = new ExecutionAttribute<>("SigningClock");

    private Aws4aSigner() {
    }

    public static Aws4aSigner create() {
        return new Aws4aSigner();
    }
}

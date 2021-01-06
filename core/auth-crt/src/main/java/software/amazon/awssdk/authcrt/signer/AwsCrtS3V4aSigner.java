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

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.authcrt.signer.internal.DefaultAwsCrtS3V4aSigner;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.core.signer.Signer;

/**
 * Enables signing and presigning for S3 using Sigv4a (Asymmetric Sigv4) through an external API call to the AWS CRT
 *  (Common RunTime) library.
 * <p/><b>S3 signing specifics</b><br>
 * For S3, the header "x-amz-sha256" must always be set for a request.
 * <p/>
 * S3 signs the payload signing if:
 * <ol>
 * <li> there's a body and an insecure protocol (HTTP) is used.</li>
 * <li> explicitly asked to via configuration/interceptor.</li>
 * </ol>
 * Otherwise, the body hash value will be UNSIGNED-PAYLOAD.
 *  <p/>
 *  See <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-authenticating-requests.html">
 *      Amazon S3 Sigv4 documentation</a> for more detailed information.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public interface AwsCrtS3V4aSigner extends Signer, Presigner {

    /**
     * Create a default AwsS34aSigner.
     */
    static AwsCrtS3V4aSigner create() {
        return DefaultAwsCrtS3V4aSigner.create();
    }
}

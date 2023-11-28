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

package software.amazon.awssdk.http.auth.aws.signer;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.OptionalDependencyLoaderUtil.getDefaultAwsCrtV4aHttpSigner;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An {@link HttpSigner} that will sign a request using an AWS credentials {@link AwsCredentialsIdentity}).
 * <p>
 * The process for signing requests to send to AWS services is documented
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_aws-signing.html">here</a>.
 */
@SdkPublicApi
public interface AwsV4aHttpSigner extends AwsV4FamilyHttpSigner<AwsCredentialsIdentity> {
    /**
     * The AWS region-set to be used for computing the signature. This property is required.
     */
    SignerProperty<RegionSet> REGION_SET = SignerProperty.create(AwsV4aHttpSigner.class, "RegionSet");

    /**
     * Get a default implementation of a {@link AwsV4aHttpSigner}
     */
    static AwsV4aHttpSigner create() {
        return getDefaultAwsCrtV4aHttpSigner();
    }

}

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
 * An {@link HttpSigner} that will use the AWS V4a signing algorithm to sign a request using an
 * {@link AwsCredentialsIdentity}).
 *
 * <p>
 * AWS request signing is described
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_aws-signing.html">here</a>.
 *
 * <h2>Using the AwsV4aHttpSigner</h2>
 * <p>
 * <b>Sign an HTTP request and send it to a service.</b>
 * <p>
 * {@snippet :
 *    AwsV4aHttpSigner signer = AwsV4aHttpSigner.create();
 *
 *    // Specify AWS credentials. Credential providers that are used by the SDK by default are
 *    // available in the module "auth" (e.g. DefaultCredentialsProvider).
 *    AwsCredentialsIdentity credentials =
 *        AwsSessionCredentialsIdentity.create("skid", "akid", "stok");
 *
 *    // Create the HTTP request to be signed
 *    SdkHttpRequest httpRequest =
 *        SdkHttpRequest.builder()
 *                      .uri("https://s3.us-west-2.amazonaws.com/bucket/object")
 *                      .method(SdkHttpMethod.PUT)
 *                      .putHeader("Content-Type", "text/plain")
 *                      .build();
 *
 *    // Create the request payload to be signed
 *    ContentStreamProvider requestPayload =
 *        ContentStreamProvider.fromUtf8String("Hello, World!");
 *
 *    // Sign the request. Some services require custom signing configuration properties (e.g. S3).
 *    // See AwsV4aHttpSigner and AwsV4FamilyHttpSigner for the available signing options.
 *    //    Note: The S3Client class below requires a dependency on the 's3' module. Alternatively, the
 *    //    signing name can be hard-coded because it is guaranteed to not change.
 *    SignedRequest signedRequest =
 *        signer.sign(r -> r.identity(credentials)
 *                          .request(httpRequest)
 *                          .payload(requestPayload)
 *                          .putProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, S3Client.SERVICE_NAME)
 *                          .putProperty(AwsV4aHttpSigner.REGION_SET, RegionSet.create("us-west-2"))
 *                          .putProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, false) // Required for S3 only
 *                          .putProperty(AwsV4aHttpSigner.NORMALIZE_PATH, false)); // Required for S3 only
 *
 *    // Create and HTTP client and send the request. ApacheHttpClient requires the 'apache-client' module.
 *    try (SdkHttpClient httpClient = ApacheHttpClient.create()) {
 *        HttpExecuteRequest httpExecuteRequest =
 *            HttpExecuteRequest.builder()
 *                              .request(signedRequest.request())
 *                              .contentStreamProvider(signedRequest.payload().orElse(null))
 *                              .build();
 *
 *        HttpExecuteResponse httpResponse =
 *            httpClient.prepareRequest(httpExecuteRequest).call();
 *
 *        System.out.println("HTTP Status Code: " + httpResponse.httpResponse().statusCode());
 *    } catch (IOException e) {
 *        System.err.println("HTTP Request Failed.");
 *        e.printStackTrace();
 *    }
 * }
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

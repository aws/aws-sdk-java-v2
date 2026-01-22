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

package software.amazon.awssdk.http.auth.spi.scheme;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;

/**
 * A marker interface for an auth scheme provider. An auth scheme provider takes as input a set of service-specific parameters,
 * and resolves a list of {@link AuthSchemeOption} based on the given parameters.
 *
 * <p>
 * <b>Customizing Signer Properties</b>
 * <p>
 * If you need to override specific {@link SignerProperty} values (such as signing name or region),
 * it is recommended to wrap the service's default auth scheme provider and update properties on the
 * resolved {@link AuthSchemeOption}s. This approach is simpler than implementing a custom {@code HttpSigner}.
 *
 * <p>
 * Example - Overriding the service signing name:
 *
 * {@snippet :
 *
 *   S3AsyncClient s3 = S3AsyncClient.builder()
 *                                  .region(Region.US_WEST_2)
 *                                  .credentialsProvider(CREDENTIALS)
 *                                  .authSchemeProvider(new CustomSigningNameAuthSchemeProvider())
 *                                  .build();
 *
 *  public class CustomSigningNameAuthSchemeProvider implements S3AuthSchemeProvider {
 *
 *     private final S3AuthSchemeProvider delegate;
 *
 *     public CustomSigningNameAuthSchemeProvider() {
 *         this.delegate = S3AuthSchemeProvider.defaultProvider();
 *     }
 *
 *     @Override
 *     public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
 *         List<AuthSchemeOption> options = delegate.resolveAuthScheme(authSchemeParams);
 *         return options.stream()
 *                       .map(option -> option.toBuilder()
 *                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "MyService")
 *                                            .build())
 *                       .collect(Collectors.toCollection(() -> new ArrayList<>(options.size())));
 *     }
 * }
 *
 * }
 */
@SdkPublicApi
public interface AuthSchemeProvider {
}

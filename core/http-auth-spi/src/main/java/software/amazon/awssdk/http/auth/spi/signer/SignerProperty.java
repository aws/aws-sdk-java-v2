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

package software.amazon.awssdk.http.auth.spi.signer;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A strongly-typed property for input to an {@link HttpSigner}.
 *
 * <p>
 * Signer properties are used to configure signing behavior by passing parameters to signers through
 * {@link software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption}s. Common properties include signing region,
 * service name, and signing algorithm parameters.
 *
 * <p>
 * <b>Common Built-in Properties</b>
 * <p>
 * The SDK provides several built-in AWS V4 signer properties:
 * <ul>
 * <li>{@code AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME} - The service name to use in the signature</li>
 * <li>{@code AwsV4FamilyHttpSigner.REGION_NAME} - The AWS region for signing</li>
 * <li>{@code AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE} - Whether to double URL-encode the path</li>
 * <li>{@code AwsV4FamilyHttpSigner.NORMALIZE_PATH} - Whether to normalize the request path</li>
 * <li>{@code AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED} - Whether to indicate that a payload is signed or not.</li>
 * </ul>
 *
 * <p>
 * <b>Overriding Properties via AuthSchemeProvider</b>
 * <p>
 * To override signer properties, implement a custom {@link software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider}
 * that wraps the default provider and modifies the properties on resolved
 * {@link software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption}s.
 *
 * <p>
 * Example - Overriding service signing name:
 *
 * {@snippet :
 * S3AsyncClient s3 = S3AsyncClient.builder()
 *                                 .region(Region.US_WEST_2)
 *                                 .credentialsProvider(CREDENTIALS)
 *                                 .authSchemeProvider(new CustomSigningNameAuthSchemeProvider())
 *                                 .build();
 *
 * public class CustomSigningNameAuthSchemeProvider implements S3AuthSchemeProvider {
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
 *                                            .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "custom-service")
 *                                            .build())
 *                       .collect(Collectors.toList());
 *     }
 * }
 * }
 *
 * <p>
 * <b>Creating Custom Properties</b>
 * <p>
 * When implementing a custom {@link HttpSigner}, you can define your own properties to accept additional configuration.
 * Properties should be defined as public static constants in your signer class.
 *
 * <p>
 * Example - Custom signer with custom property:
 *
 * {@snippet :
 * public class CustomHttpSigner implements HttpSigner<AwsCredentialsIdentity> {
 *     // Define custom property
 *     public static final SignerProperty<String> CUSTOM_HEADER_NAME =
 *         SignerProperty.create(CustomHttpSigner.class, "CustomHeaderName");
 *
 *     @Override
 *     public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
 *         // Retrieve property value
 *         String headerName = request.property(CUSTOM_HEADER_NAME);
 *         // Use the property in signing logic
 *         // ...
 *     }
 * }
 *
 * // Configure the property via AuthSchemeProvider
 * public class CustomPropertyAuthSchemeProvider implements S3AuthSchemeProvider {
 *     private final S3AuthSchemeProvider delegate;
 *
 *     public CustomPropertyAuthSchemeProvider() {
 *         this.delegate = S3AuthSchemeProvider.defaultProvider();
 *     }
 *
 *     @Override
 *     public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
 *         List<AuthSchemeOption> options = delegate.resolveAuthScheme(authSchemeParams);
 *         return options.stream()
 *                       .map(option -> option.toBuilder()
 *                                            .putSignerProperty(CustomHttpSigner.CUSTOM_HEADER_NAME, "X-Custom-Auth")
 *                                            .build())
 *                       .collect(Collectors.toList());
 *     }
 * }
 * }
 *
 * @param <T> The type of the property.
 * @see HttpSigner
 * @see software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption
 * @see software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class SignerProperty<T> {
    private static final ConcurrentMap<Pair<String, String>, SignerProperty<?>> NAME_HISTORY = new ConcurrentHashMap<>();

    private final String namespace;
    private final String name;

    private SignerProperty(String namespace, String name) {
        Validate.paramNotBlank(namespace, "namespace");
        Validate.paramNotBlank(name, "name");

        this.namespace = namespace;
        this.name = name;
        ensureUnique();
    }

    /**
     * Create a property.
     *
     * @param <T> the type of the property.
     * @param namespace the class *where* the property is being defined
     * @param name the name for the property
     * @throws IllegalArgumentException if a property with this namespace and name already exist
     */
    public static <T> SignerProperty<T> create(Class<?> namespace, String name) {
        return new SignerProperty<>(namespace.getName(), name);
    }

    private void ensureUnique() {
        SignerProperty<?> prev = NAME_HISTORY.putIfAbsent(Pair.of(namespace, name), this);
        Validate.isTrue(prev == null,
                        "No duplicate SignerProperty names allowed but both SignerProperties %s and %s have the same namespace "
                        + "(%s) and name (%s). SignerProperty should be referenced from a shared static constant to protect "
                        + "against erroneous or unexpected collisions.",
                        Integer.toHexString(System.identityHashCode(prev)),
                        Integer.toHexString(System.identityHashCode(this)),
                        namespace,
                        name);
    }

    @Override
    public String toString() {
        return ToString.builder("SignerProperty")
                       .add("namespace", namespace)
                       .add("name", name)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SignerProperty<?> that = (SignerProperty<?>) o;

        return Objects.equals(namespace, that.namespace) &&
               Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(namespace);
        hashCode = 31 * hashCode + Objects.hashCode(name);
        return hashCode;
    }
}

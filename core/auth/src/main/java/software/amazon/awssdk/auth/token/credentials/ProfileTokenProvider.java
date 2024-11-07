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

package software.amazon.awssdk.auth.token.credentials;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.auth.token.internal.ProfileTokenProviderLoader;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;

/**
 * An {@link IdentityProvider}{@code <}{@link TokenIdentity}{@code >} that loads tokens from a
 * {@link ProfileFile} in {@code ~/.aws/config} and {@code ~/.aws/credentials}.
 *
 * <p>
 * This token provider reads the profile once, and will not be updated if the file is changed. To monitor the file
 * for updates, you can provide a {@link ProfileFileSupplier} with {@link Builder#profileFile(Supplier)}.
 *
 * <p>
 * This provider processes the profile files and delegate its configuration to other token providers. For a full guide on
 * how to configure tokens using a profile file, see
 * <a href="https://docs.aws.amazon.com/sdkref/latest/guide/file-format.html">the configuration file guide</a>.
 * The SDK determines which token provider to delegate to based on the following ordered logic:
 * <ol>
 *     <li><b>{@link software.amazon.awssdk.services.ssooidc.SsoOidcTokenProvider}</b>: Used if the file contains {@code
 *     sso_session} (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sso">{@code sso}</a>)</li>
 * </ol>
 *
 * <p>
 * Many token providers in this chain will make service calls to retrieve tokens. These providers will cache the
 * token result, and will only invoke the service periodically to keep the token "fresh". As a result, it is
 * recommended that you create a single tokens provider of this type and reuse it throughout your application. You may
 * notice small latency increases on requests that refresh the cached tokens.
 *
 * <p>
 * You should {@link #close()} this token provider if you are done using it, because some configurations can cause the
 * creation of resources that cannot be garbage collected.
 *
 * <p>
 * There are system properties and environment variables that can control the behavior of this token provider:
 * <ul>
 *     <li>The {@code aws.configFile} system property or {@code AWS_CONFIG_FILE} environment
 *     variable can be set to override the default location of the config file ({@code ~/.aws/config}).</li>
 *     <li>The {@code aws.sharedCredentialsFile} system property or {@code AWS_SHARED_CREDENTIALS_FILE} environment
 *     variable can be set to override the default location of the credentials file ({@code ~/.aws/credentials}).</li>
 *     <li>The {@code aws.profile} system property or {@code AWS_PROFILE} environment
 *     variable can be set to override the default profile used (literally, {@code default}).</li>
 *     <li>The {@code HOME} environment variable can be set to override the way the SDK interprets {@code ~/} in the
 *     configuration file or credentials file location. If {@code HOME} is not set, on Windows the SDK will also check
 *     {@code USERPROFILE}, and {@code HOMEDRIVE} + {@code HOMEPATH}. If none of these are set, on all platforms the SDK will
 *     then use the {@code user.home} system property.</li>
 * </ul>
 * <p>
 * This tokens provider is included in the {@link DefaultAwsTokenProvider}.
 * <p>
 * This can be created using {@link #create()} or {@link #builder()}:
 * {@snippet :
 * ProfileTokenProvider tokenProvider =
 *    ProfileTokenProvider.create(); // @link substring="create" target="#create()"
 *
 * // or
 *
 * ProfileTokenProvider tokenProvider =
 *     ProfileTokenProvider.create("custom-profile-name");  // @link substring="create" target="#create(String)"
 *
 * // or
 *
 * ProfileTokenProvider tokenProvider =
 *     ProfileTokenProvider.builder() // @link substring="builder" target="#builder()"
 *                         .profileFile(ProfileFile.defaultProfileFile())
 *                         .profileName("custom-profile-name")
 *                         .build();
 *
 * ServiceClient service = ServiceClient.builder()
 *                                      .tokenProvider(tokenProvider)
 *                                      .build();
 * }
 */
@SdkPublicApi
public final class ProfileTokenProvider implements SdkTokenProvider, SdkAutoCloseable {
    private final SdkTokenProvider tokenProvider;
    private final RuntimeException loadException;

    private final String profileName;

    private ProfileTokenProvider(BuilderImpl builder) {
        SdkTokenProvider sdkTokenProvider = null;
        RuntimeException thrownException = null;
        Supplier<ProfileFile> selectedProfileFile = null;
        String selectedProfileName = null;

        try {
            selectedProfileName = Optional.ofNullable(builder.profileName)
                                          .orElseGet(ProfileFileSystemSetting.AWS_PROFILE::getStringValueOrThrow);

            // Load the profiles file
            selectedProfileFile = Optional.ofNullable(builder.profileFile)
                                          .orElse(builder.defaultProfileFileLoader);

            // Load the profile and token provider
            sdkTokenProvider = createTokenProvider(selectedProfileFile, selectedProfileName);

        } catch (RuntimeException e) {
            // If we couldn't load the provider for some reason, save an exception describing why.
            thrownException = e;
        }

        if (thrownException != null) {
            this.loadException = thrownException;
            this.tokenProvider = null;
            this.profileName = null;
        } else {
            this.loadException = null;
            this.tokenProvider = sdkTokenProvider;
            this.profileName = selectedProfileName;
        }
    }

    /**
     * Create a {@link ProfileTokenProvider} with default configuration.
     * <p>
     * {@snippet :
     * ProfileTokenProvider tokenProvider = ProfileTokenProvider.create();
     * }
     */
    public static ProfileTokenProvider create() {
        return builder().build();
    }

    /**
     * Create a {@link ProfileTokenProvider} with default configuration and the provided profile name.
     * <p>
     * {@snippet :
     * ProfileTokenProvider tokenProvider = ProfileTokenProvider.create("custom-profile-name");
     * }
     */
    public static ProfileTokenProvider create(String profileName) {
        return builder().profileName(profileName).build();
    }

    /**
     * Get a new builder for creating a {@link ProfileTokenProvider}.
     * <p>
     * {@snippet :
     * ProfileTokenProvider tokenProvider =
     *     ProfileTokenProvider.builder()
     *                         .profileName("custom-profile-name")
     *                         .build();
     * }
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public SdkToken resolveToken() {
        if (loadException != null) {
            throw loadException;
        }
        return tokenProvider.resolveToken();
    }

    @Override
    public String toString() {
        return ToString.builder("ProfileTokenProvider")
                       .add("profileName", profileName)
                       .build();
    }

    /**
     * Release resources held by this token provider. This should be called when you're done using the token
     * provider, because some delegate providers hold resources (e.g. clients) that must be released.
     */
    @Override
    public void close() {
        IoUtils.closeIfCloseable(tokenProvider, null);
    }

    private SdkTokenProvider createTokenProvider(Supplier<ProfileFile> profileFile, String profileName) {
        return new ProfileTokenProviderLoader(profileFile, profileName)
            .tokenProvider()
            .orElseThrow(() -> {
                String errorMessage = String.format("Profile file contained no information for " +
                                                    "profile '%s'", profileName);
                return SdkClientException.builder().message(errorMessage).build();
            });
    }

    /**
     * See {@link ProfileTokenProvider} for detailed documentation.
     */
    public interface Builder {
        /**
         * Define the {@link ProfileFile} that should be used by this token provider.
         *
         * <p>
         * The profile file is only read when the {@link ProfileFile} object is created, so the token provider will not
         * reflect any changes made in the provided file. To automatically adjust to changes in the file, see
         * {@link #profileFile(Supplier)}.
         *
         * <p>
         * If not specified, the {@link ProfileFile#defaultProfileFile()} will be used.
         *
         * <p>
         * {@snippet :
         * ProfileTokenProvider.builder()
         *                     .profileFile(ProfileFile.builder()
         *                                             .type(ProfileFile.Type.CONFIGURATION)
         *                                             .content(Paths.get("~/.aws/config"))
         *                                             .build())
         *                     .build()
         *}
         *
         * @see ProfileFile
         */
        Builder profileFile(ProfileFile profileFile);

        /**
         * Define a {@link ProfileFileSupplier} that should be used by this token provider.
         *
         * <p>
         * The profile file supplier is called each time the {@link ProfileFile} is read, so the token provider can
         * "pick up" changes made in the provided file.
         *
         * <p>
         * If not specified, the (fixed) {@link ProfileFile#defaultProfileFile()} will be used.
         *
         * <p>
         * {@snippet :
         * ProfileTokenProvider.builder()
         *                     .profileFile(ProfileFileSupplier.defaultSupplier())
         *                     .build()
         *}
         *
         * @see ProfileFileSupplier
         */
        Builder profileFile(Supplier<ProfileFile> profileFile);

        /**
         * Define the name of the profile that should be used by this token provider.
         *
         * <p>
         * If this profile does not exist in the {@link ProfileFile}, token resolution will fail.
         *
         * <p>
         * If not specified, the {@code aws.profile} system property or {@code AWS_PROFILE} environment variable's value will
         * be used. If these are not set, then {@code default} will be used.
         *
         * <p>
         * {@snippet :
         * ProfileTokenProvider.builder()
         *                     .profileName("custom-profile-name")
         *                     .build()
         *}
         */
        Builder profileName(String profileName);

        /**
         * Build the {@link ProfileTokenProvider}.
         *
         * <p>
         * {@snippet :
         * ProfileTokenProvider tokenProvider =
         *     ProfileTokenProvider.builder()
         *                         .profileName("custom-profile-name")
         *                         .build();
         * }
         */
        ProfileTokenProvider build();
    }

    static final class BuilderImpl implements Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        private Supplier<ProfileFile> defaultProfileFileLoader = ProfileFile::defaultProfileFile;

        BuilderImpl() {
        }

        @Override
        public Builder profileFile(ProfileFile profileFile) {
            this.profileFile = () -> profileFile;
            return this;
        }

        @Override
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public void setProfileFile(Supplier<ProfileFile> profileFile) {
            profileFile(profileFile);
        }

        @Override
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public void setProfileName(String profileName) {
            profileName(profileName);
        }

        @Override
        public ProfileTokenProvider build() {
            return new ProfileTokenProvider(this);
        }

        /**
         * Override the default configuration file to be used when the customer does not explicitly set
         * profileName(profileName);
         * {@link #profileFile(Supplier<ProfileFile>)}. Use of this method is only useful for testing the default behavior.
         */
        @SdkTestInternalApi
        Builder defaultProfileFileLoader(Supplier<ProfileFile> defaultProfileFileLoader) {
            this.defaultProfileFileLoader = defaultProfileFileLoader;
            return this;
        }
    }
}

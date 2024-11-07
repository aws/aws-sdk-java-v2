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

package software.amazon.awssdk.auth.credentials;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileLocation;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} that loads credentials from a
 * {@link ProfileFile} in {@code ~/.aws/config} and {@code ~/.aws/credentials}.
 *
 * <p>
 * This credential provider reads the profile once, and will not be updated if the file is changed. To monitor the file
 * for updates, you can provide a {@link ProfileFileSupplier} with {@link Builder#profileFile(Supplier)}.
 *
 * <p>
 * This provider processes the profile files and delegate its configuration to other credential providers. For a full guide on
 * how to configure SDK credentials using a profile file, see
 * <a href="https://docs.aws.amazon.com/sdkref/latest/guide/file-format.html">the configuration file guide</a>.
 * The SDK determines which credential provider to delegate to based on the following ordered logic:
 * <ol>
 *     <li><b>{@link WebIdentityTokenFileCredentialsProvider}</b>: Used if the file contains {@code role_arn} and {@code
 *     web_identity_token_file}. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.services.sso.auth.SsoCredentialsProvider}</b>: Used if the file contains
 *     {@code sso_*} properties. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sso">{@code sso}</a>)</li>
 *     <li><b>{@link software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider}</b>: Used if the file contains the
 *     {@code role_arn} property. (Requires a dependency on
 *     <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a>)</li>
 *     <li><b>{@link ProcessCredentialsProvider}</b>: Used if the file contains the {@code credential_process} property.</li>
 *     <li><b>{@link StaticCredentialsProvider}</b> with <b>{@link AwsSessionCredentialsIdentity}</b>: Used if the file contains
 *     the {@code aws_session_token} property. </li>
 *     <li><b>{@link StaticCredentialsProvider}</b> with <b>{@link AwsCredentialsIdentity}</b>: Used if the file contains the
 *     {@code aws_access_key_id} property.</li>
 * </ol>
 *
 * <p>
 * Many credential providers in this chain will make service calls to retrieve credentials. These providers will cache the
 * credential result, and will only invoke the service periodically to keep the credential "fresh". As a result, it is
 * recommended that you create a single credentials provider of this type and reuse it throughout your application. You may
 * notice small latency increases on requests that refresh the cached credentials.
 *
 * <p>
 * You should {@link #close()} this credential provider if you are done using it, because some configurations can cause the
 * creation of resources that cannot be garbage collected.
 *
 * <p>
 * There are system properties and environment variables that can control the behavior of this credential provider:
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
 * This credentials provider is included in the {@link DefaultCredentialsProvider}.
 * <p>
 * This can be created using {@link #create()} or {@link #builder()}:
 * {@snippet :
 * ProfileCredentialsProvider credentialsProvider =
 *    ProfileCredentialsProvider.create(); // @link substring="create" target="#create()"
 *
 * // or
 *
 * ProfileCredentialsProvider credentialsProvider =
 *     ProfileCredentialsProvider.create("custom-profile-name");  // @link substring="create" target="#create(String)"
 *
 * // or
 *
 * ProfileCredentialsProvider credentialsProvider =
 *     ProfileCredentialsProvider.builder() // @link substring="builder" target="#builder()"
 *                               .profileFile(ProfileFile.defaultProfileFile())
 *                               .profileName("custom-profile-name")
 *                               .build();
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 * }
 */
@SdkPublicApi
public final class ProfileCredentialsProvider
    implements AwsCredentialsProvider,
               SdkAutoCloseable,
               ToCopyableBuilder<ProfileCredentialsProvider.Builder, ProfileCredentialsProvider> {

    private volatile AwsCredentialsProvider credentialsProvider;
    private final RuntimeException loadException;
    private final Supplier<ProfileFile> profileFile;
    private volatile ProfileFile currentProfileFile;
    private final String profileName;
    private final Supplier<ProfileFile> defaultProfileFileLoader;

    private final Object credentialsProviderLock = new Object();

    private ProfileCredentialsProvider(BuilderImpl builder) {
        this.defaultProfileFileLoader = builder.defaultProfileFileLoader;

        RuntimeException thrownException = null;
        String selectedProfileName = null;
        Supplier<ProfileFile> selectedProfileSupplier = null;

        try {
            selectedProfileName = Optional.ofNullable(builder.profileName)
                                          .orElseGet(ProfileFileSystemSetting.AWS_PROFILE::getStringValueOrThrow);
            selectedProfileSupplier =
                Optional.ofNullable(builder.profileFile)
                        .orElseGet(() -> ProfileFileSupplier.fixedProfileFile(builder.defaultProfileFileLoader.get()));

        } catch (RuntimeException e) {
            // If we couldn't load the credentials provider for some reason, save an exception describing why. This exception
            // will only be raised on calls to resolveCredentials. We don't want to raise an exception here because it may be
            // expected (eg. in the default credential chain).
            thrownException = e;
        }

        this.loadException = thrownException;
        this.profileName = selectedProfileName;
        this.profileFile = selectedProfileSupplier;
    }

    /**
     * Create a {@link ProfileCredentialsProvider} with default configuration.
     * <p>
     * {@snippet :
     * ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
     * }
     */
    public static ProfileCredentialsProvider create() {
        return builder().build();
    }

    /**
     * Create a {@link ProfileCredentialsProvider} with default configuration and the provided profile name.
     * <p>
     * {@snippet :
     * ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create("custom-profile-name");
     * }
     */
    public static ProfileCredentialsProvider create(String profileName) {
        return builder().profileName(profileName).build();
    }

    /**
     * Get a new builder for creating a {@link ProfileCredentialsProvider}.
     * <p>
     * {@snippet :
     * ProfileCredentialsProvider credentialsProvider =
     *     ProfileCredentialsProvider.builder()
     *                               .profileName("custom-profile-name")
     *                               .build();
     * }
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        if (loadException != null) {
            throw loadException;
        }

        ProfileFile cachedOrRefreshedProfileFile = refreshProfileFile();
        if (shouldUpdateCredentialsProvider(cachedOrRefreshedProfileFile)) {
            synchronized (credentialsProviderLock) {
                if (shouldUpdateCredentialsProvider(cachedOrRefreshedProfileFile)) {
                    currentProfileFile = cachedOrRefreshedProfileFile;
                    handleProfileFileReload(cachedOrRefreshedProfileFile);
                }
            }
        }

        return credentialsProvider.resolveCredentials();
    }

    private void handleProfileFileReload(ProfileFile profileFile) {
        credentialsProvider = createCredentialsProvider(profileFile, profileName);
    }

    private ProfileFile refreshProfileFile() {
        return profileFile.get();
    }

    private boolean shouldUpdateCredentialsProvider(ProfileFile profileFile) {
        return credentialsProvider == null || !Objects.equals(currentProfileFile, profileFile);
    }

    @Override
    public String toString() {
        return ToString.builder("ProfileCredentialsProvider")
                       .add("profileName", profileName)
                       .add("profileFile", currentProfileFile)
                       .build();
    }

    /**
     * Release resources held by this credentials provider. This should be called when you're done using the credentials
     * provider, because some delegate providers hold resources (e.g. clients) that must be released.
     */
    @Override
    public void close() {
        IoUtils.closeIfCloseable(credentialsProvider, null);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    private AwsCredentialsProvider createCredentialsProvider(ProfileFile profileFile, String profileName) {
        // Load the profile and credentials provider
        return profileFile.profile(profileName)
                          .flatMap(p -> new ProfileCredentialsUtils(profileFile, p, profileFile::profile).credentialsProvider())
                          .orElseThrow(() -> {
                              String errorMessage = String.format("Profile file contained no credentials for " +
                                                                  "profile '%s': %s", profileName, profileFile);
                              return SdkClientException.builder().message(errorMessage).build();
                          });
    }

    /**
     * See {@link ProfileCredentialsProvider} for detailed documentation.
     */
    public interface Builder extends CopyableBuilder<Builder, ProfileCredentialsProvider> {
        /**
         * Define the {@link ProfileFile} that should be used by this credentials provider.
         *
         * <p>
         * The profile file is only read when the {@link ProfileFile} object is created, so the credentials provider will not
         * reflect any changes made in the provided file. To automatically adjust to changes in the file, see
         * {@link #profileFile(Supplier)}.
         *
         * <p>
         * If not specified, the {@link ProfileFile#defaultProfileFile()} will be used.
         *
         * <p>
         * {@snippet :
         * ProfileCredentialsProvider.builder()
         *                           .profileFile(ProfileFile.builder()
         *                                                   .type(ProfileFile.Type.CONFIGURATION)
         *                                                   .content(Paths.get("~/.aws/config"))
         *                                                   .build())
         *                           .build()
         *}
         *
         * @see ProfileFile
         */
        Builder profileFile(ProfileFile profileFile);

        /**
         * Define the {@link ProfileFile} that should be used by this credentials provider.
         *
         * <p>
         * Similar to {@link #profileFile(ProfileFile)}, but takes a lambda to configure a new {@link ProfileFile.Builder}.
         * This removes the need to called {@link ProfileFile#builder()} and {@link ProfileFile.Builder#build()}.
         *
         * <p>
         * The profile file is only read when the {@link ProfileFile} object is created, so the credentials provider will not
         * reflect any changes made in the provided file. To automatically adjust to changes in the file, see
         * {@link #profileFile(Supplier)}.
         *
         * <p>
         * If not specified, the {@link ProfileFile#defaultProfileFile()} will be used.
         *
         * <p>
         * {@snippet :
         * ProfileCredentialsProvider.builder()
         *                           .profileFile(file -> file.type(ProfileFile.Type.CONFIGURATION)
         *                                                    .content(Paths.get("~/.aws/config")))
         *                           .build()
         *}
         *
         * @see ProfileFile
         */
        Builder profileFile(Consumer<ProfileFile.Builder> profileFile);

        /**
         * Define a {@link ProfileFileSupplier} that should be used by this credentials provider.
         *
         * <p>
         * The profile file supplier is called each time the {@link ProfileFile} is read, so the credentials provider can
         * "pick up" changes made in the provided file.
         *
         * <p>
         * If not specified, the (fixed) {@link ProfileFile#defaultProfileFile()} will be used.
         *
         * <p>
         * {@snippet :
         * ProfileCredentialsProvider.builder()
         *                           .profileFile(ProfileFileSupplier.defaultSupplier())
         *                           .build()
         *}
         *
         * @see ProfileFileSupplier
         */
        Builder profileFile(Supplier<ProfileFile> profileFileSupplier);

        /**
         * Define the name of the profile that should be used by this credentials provider.
         *
         * <p>
         * If this profile does not exist in the {@link ProfileFile}, credential resolution will fail.
         *
         * <p>
         * If not specified, the {@code aws.profile} system property or {@code AWS_PROFILE} environment variable's value will
         * be used. If these are not set, then {@code default} will be used.
         *
         * <p>
         * {@snippet :
         * ProfileCredentialsProvider.builder()
         *                           .profileName("custom-profile-name")
         *                           .build()
         *}
         */
        Builder profileName(String profileName);

        /**
         * Build the {@link ProfileCredentialsProvider}.
         *
         * <p>
         * {@snippet :
         * ProfileCredentialsProvider credentialsProvider =
         *     ProfileCredentialsProvider.builder()
         *                               .profileName("custom-profile-name")
         *                               .build();
         * }
         */
        @Override
        ProfileCredentialsProvider build();
    }

    static final class BuilderImpl implements Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private Supplier<ProfileFile> defaultProfileFileLoader = ProfileFile::defaultProfileFile;

        BuilderImpl() {
        }

        BuilderImpl(ProfileCredentialsProvider provider) {
            this.profileName = provider.profileName;
            this.defaultProfileFileLoader = provider.defaultProfileFileLoader;
            this.profileFile = provider.profileFile;
        }

        @Override
        public Builder profileFile(ProfileFile profileFile) {
            return profileFile(Optional.ofNullable(profileFile)
                                       .map(ProfileFileSupplier::fixedProfileFile)
                                       .orElse(null));
        }

        public void setProfileFile(ProfileFile profileFile) {
            profileFile(profileFile);
        }

        @Override
        public Builder profileFile(Consumer<ProfileFile.Builder> profileFile) {
            return profileFile(ProfileFile.builder().applyMutation(profileFile).build());
        }

        @Override
        public Builder profileFile(Supplier<ProfileFile> profileFileSupplier) {
            this.profileFile = profileFileSupplier;
            return this;
        }

        public void setProfileFile(Supplier<ProfileFile> supplier) {
            profileFile(supplier);
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
        public ProfileCredentialsProvider build() {
            return new ProfileCredentialsProvider(this);
        }

        /**
         * Override the default configuration file to be used when the customer does not explicitly set
         * profileFile(ProfileFile) or profileFileSupplier(supplier);
         * {@link #profileFile(ProfileFile)}. Use of this method is
         * only useful for testing the default behavior.
         */
        @SdkTestInternalApi
        Builder defaultProfileFileLoader(Supplier<ProfileFile> defaultProfileFileLoader) {
            this.defaultProfileFileLoader = defaultProfileFileLoader;
            return this;
        }
    }

}

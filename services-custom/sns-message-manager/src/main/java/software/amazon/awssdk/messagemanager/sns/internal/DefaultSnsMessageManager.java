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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.io.InputStream;
import java.net.URI;
import java.security.PublicKey;
import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultSnsMessageManager implements SnsMessageManager {
    private static final String SIGNING_CERT_URL = "SigningCertURL";

    private static final AttributeMap HTTP_CLIENT_DEFAULTS =
        AttributeMap.builder()
                    .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofSeconds(10))
                    .put(SdkHttpConfigurationOption.READ_TIMEOUT, Duration.ofSeconds(30))
                    .build();

    private final SnsMessageUnmarshaller unmarshaller;
    private final CertificateRetriever certRetriever;
    private final SignatureValidator signatureValidator;
    private final SnsEndpointUrlValidator endpointUrlValidator;

    private DefaultSnsMessageManager(BuilderImpl builder) {
        this.unmarshaller = new SnsMessageUnmarshaller();
        this.signatureValidator = new SignatureValidator(builder.minimumSignatureVersion);

        SnsHostProvider hostProvider = new SnsHostProvider(builder.region);
        URI signingCertEndpoint = hostProvider.regionalEndpoint();
        String signingCertCommonName = hostProvider.signingCertCommonName();

        SdkHttpClient httpClient = resolveHttpClient(builder);
        certRetriever = builder.certRetriever != null
                        ? builder.certRetriever
                        : new CertificateRetriever(httpClient, signingCertEndpoint.getHost(), signingCertCommonName);

        endpointUrlValidator = new SnsEndpointUrlValidator(signingCertEndpoint.getHost());
    }

    @Override
    public SnsMessage parseMessage(InputStream message) {
        Validate.notNull(message, "message cannot be null");
        return validate(unmarshaller.unmarshall(message));
    }

    @Override
    public SnsMessage parseMessage(String message) {
        Validate.notNull(message, "message cannot be null");
        return validate(unmarshaller.unmarshall(message));
    }

    private SnsMessage validate(SnsMessage snsMessage) {
        URI certificateUrl = snsMessage.signingCertUrl();
        endpointUrlValidator.validate(certificateUrl, SIGNING_CERT_URL);

        PreparedSignature prepared = signatureValidator.prepare(snsMessage);

        // Try the cached certificates first; the URL is only fetched if none of them verifies the signature.
        for (PublicKey cached : certRetriever.cachedCertificates(certificateUrl)) {
            if (prepared.verifies(cached)) {
                return snsMessage;
            }
        }

        if (!prepared.verifies(certRetriever.retrieveCertificate(certificateUrl))) {
            throw SdkClientException.create("The computed signature did not match the expected signature");
        }

        return snsMessage;
    }

    @Override
    public void close() {
        certRetriever.close();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    private static SdkHttpClient resolveHttpClient(BuilderImpl builder) {
        if (builder.httpClient != null) {
            return new UnmanagedSdkHttpClient(builder.httpClient);
        }

        return new DefaultSdkHttpClientBuilder().buildWithDefaults(HTTP_CLIENT_DEFAULTS);
    }

    static class BuilderImpl implements SnsMessageManager.Builder {
        private Region region;
        private SdkHttpClient httpClient;
        private SignatureVersion minimumSignatureVersion = SignatureVersion.VERSION_1;

        // Testing only
        private CertificateRetriever certRetriever;

        @Override
        public Builder httpClient(SdkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder minimumSignatureVersion(SignatureVersion minimumSignatureVersion) {
            this.minimumSignatureVersion = minimumSignatureVersion;
            return this;
        }

        @SdkTestInternalApi
        Builder certificateRetriever(CertificateRetriever certificateRetriever) {
            this.certRetriever = certificateRetriever;
            return this;
        }

        @Override
        public SnsMessageManager build() {
            return new DefaultSnsMessageManager(this);
        }
    }
}

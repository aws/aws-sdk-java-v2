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

package software.amazon.awssdk.services.polly.presigner;

import java.net.URI;
import java.net.URLConnection;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.internal.presigner.DefaultPollyPresigner;
import software.amazon.awssdk.services.polly.model.PollyRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.presigner.model.PresignedSynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.presigner.model.SynthesizeSpeechPresignRequest;

/**
 * Enables signing a {@link PollyRequest} so that it can be executed without requiring any additional authentication on the
 * part of the caller.
 * <p/>
 *
 * <b>Signature Duration</b>
 * <p/>
 *
 * Pre-signed requests are only valid for a finite period of time, referred to as the signature duration. This signature
 * duration is configured when the request is generated, and cannot be longer than 7 days. Attempting to generate a signature
 * longer than 7 days in the future will fail at generation time. Attempting to use a pre-signed request after the signature
 * duration has passed will result in an access denied response from the service.
 * <p/>
 *
 * <b>Example Usage</b>
 * <p/>
 *
 * <pre>
 * {@code
 *     // Create a PollyPresigner using the default region and credentials.
 *     // This is usually done at application startup, because creating a presigner can be expensive.
 *     PollyPresigner presigner = PollyPresigner.create();
 *
 *     // Create a SynthesizeSpeechRequest to be pre-signed
 *     SynthesizeSpeechRequest synthesizeSpeechRequest =
 *             SynthesizeSpeechRequest.builder()
 *                                    .text("Hello Polly!")
 *                                    .voiceId(VoiceId.SALLI)
 *                                    .outputFormat(OutputFormat.PCM)
 *                                    .build();
 *
 *     // Create a SynthesizeSpeechRequest to specify the signature duration
 *     SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
 *         SynthesizeSpeechPresignRequest.builder()
 *                                .signatureDuration(Duration.ofMinutes(10))
 *                                .synthesizeSpeechRequest(synthesizeSpeechRequest)
 *                                .build();
 *
 *     // Generate the presigned request
 *     PresignedSynthesizeSpeechRequest presignedSynthesizeSpeechRequest =
 *         presigner.presignSynthesizeSpeech(SynthesizeSpeechPresignRequest);
 *
 *     // Log the presigned URL, for example.
 *     System.out.println("Presigned URL: " + presignedSynthesizeSpeechRequest.url());
 *
 *     // It is recommended to close the presigner when it is done being used, because some credential
 *     // providers (e.g. if your AWS profile is configured to assume an STS role) require system resources
 *     // that need to be freed. If you are using one presigner per application (as recommended), this
 *     // usually is not needed.
 *     presigner.close();
 * }
 * </pre>
 * <p/>
 *
 * <b>Browser Compatibility</b>
 * <p/>
 *
 * Some pre-signed requests can be executed by a web browser. These "browser compatible" pre-signed requests
 * do not require the customer to send anything other than a "host" header when performing an HTTP GET against
 * the pre-signed URL.
 * <p/>
 *
 * Whether a pre-signed request is "browser compatible" can be determined by checking the
 * {@link PresignedRequest#isBrowserExecutable()} flag. It is recommended to always check this flag when the pre-signed
 * request needs to be executed by a browser, because some request fields will result in the pre-signed request not
 * being browser-compatible.
 * <p />
 *
 * <b>Executing a Pre-Signed Request from Java code</b>
 * <p />
 *
 * Browser-compatible requests (see above) can be executed using a web browser. All pre-signed requests can be executed
 * from Java code. This documentation describes two methods for executing a pre-signed request: (1) using the JDK's
 * {@link URLConnection} class, (2) using an SDK synchronous {@link SdkHttpClient} class.
 *
 * <p />
 * <i>Using {code URLConnection}:</i>
 *
 * <p />
 * <pre>
 *     // Create a pre-signed request using one of the "presign" methods on PollyPresigner
 *     PresignedRequest presignedRequest = ...;
 *
 *     // Create a JDK HttpURLConnection for communicating with Polly
 *     HttpURLConnection connection = (HttpURLConnection) presignedRequest.url().openConnection();
 *
 *     // Specify any headers that are needed by the service (not needed when isBrowserExecutable is true)
 *     presignedRequest.httpRequest().headers().forEach((header, values) -> {
 *         values.forEach(value -> {
 *             connection.addRequestProperty(header, value);
 *         });
 *     });
 *
 *     // Download the result of executing the request
 *     try (InputStream content = connection.getInputStream()) {
 *         System.out.println("Service returned response: ");
 *         IoUtils.copy(content, myFileOutputstream);
 *     }
 * </pre>
 */
@SdkPublicApi
public interface PollyPresigner extends SdkPresigner {

    /**
     * Presign a {@link SynthesizeSpeechRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     *
     * @param synthesizeSpeechPresignRequest The presign request.
     * @return The presigned request.
     */
    PresignedSynthesizeSpeechRequest presignSynthesizeSpeech(SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest);

    /**
     * Create an instance of this presigner using the default region and credentials chains to resolve the region and
     * credentials to use.
     */
    static PollyPresigner create() {
        return builder().build();
    }

    /**
     * @return the builder for a {@link PollyPresigner}.
     */
    static Builder builder() {
        return DefaultPollyPresigner.builder();
    }

    interface Builder extends SdkPresigner.Builder {
        @Override
        Builder region(Region region);

        @Override
        Builder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        @Override
        Builder endpointOverride(URI endpointOverride);

        PollyPresigner build();
    }
}
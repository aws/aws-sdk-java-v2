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

package software.amazon.awssdk.services.s3.presigner;

import java.net.URI;
import java.net.URLConnection;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.presigner.DefaultS3Presigner;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.AbortMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.CompleteMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.CreateMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedAbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

/**
 * Enables signing an S3 {@link SdkRequest} so that it can be executed without requiring any additional authentication on the
 * part of the caller.
 * <p/>
 *
 * For example: if Alice has access to an S3 object, and she wants to temporarily share access to that object with Bob, she
 * can generate a pre-signed {@link GetObjectRequest} to secure share with Bob so that he can download the object without
 * requiring access to Alice's credentials.
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
 *     // Create an S3Presigner using the default region and credentials.
 *     // This is usually done at application startup, because creating a presigner can be expensive.
 *     S3Presigner presigner = S3Presigner.create();
 *
 *     // Create a GetObjectRequest to be pre-signed
 *     GetObjectRequest getObjectRequest =
 *             GetObjectRequest.builder()
 *                             .bucket("my-bucket")
 *                             .key("my-key")
 *                             .build();
 *
 *     // Create a GetObjectPresignRequest to specify the signature duration
 *     GetObjectPresignRequest getObjectPresignRequest =
 *         GetObjectPresignRequest.builder()
 *                                .signatureDuration(Duration.ofMinutes(10))
 *                                .getObjectRequest(request)
 *                                .build();
 *
 *     // Generate the presigned request
 *     PresignedGetObjectRequest presignedGetObjectRequest =
 *         presigner.presignGetObject(getObjectPresignRequest);
 *
 *     // Log the presigned URL, for example.
 *     System.out.println("Presigned URL: " + presignedGetObjectRequest.url());
 *
 *     // It is recommended to close the S3Presigner when it is done being used, because some credential
 *     // providers (e.g. if your AWS profile is configured to assume an STS role) require system resources
 *     // that need to be freed. If you are using one S3Presigner per application (as recommended), this
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
 *     // Create a pre-signed request using one of the "presign" methods on S3Presigner
 *     PresignedRequest presignedRequest = ...;
 *
 *     // Create a JDK HttpURLConnection for communicating with S3
 *     HttpURLConnection connection = (HttpURLConnection) presignedRequest.url().openConnection();
 *
 *     // Specify any headers that are needed by the service (not needed when isBrowserExecutable is true)
 *     presignedRequest.httpRequest().headers().forEach((header, values) -> {
 *         values.forEach(value -> {
 *             connection.addRequestProperty(header, value);
 *         });
 *     });
 *
 *     // Send any request payload that is needed by the service (not needed when isBrowserExecutable is true)
 *     if (presignedRequest.signedPayload().isPresent()) {
 *         connection.setDoOutput(true);
 *         try (InputStream signedPayload = presignedRequest.signedPayload().get().asInputStream();
 *              OutputStream httpOutputStream = connection.getOutputStream()) {
 *             IoUtils.copy(signedPayload, httpOutputStream);
 *         }
 *     }
 *
 *     // Download the result of executing the request
 *     try (InputStream content = connection.getInputStream()) {
 *         System.out.println("Service returned response: ");
 *         IoUtils.copy(content, System.out);
 *     }
 * </pre>
 * <p />
 *
 * <i>Using {code SdkHttpClient}:</i>
 * <p />
 *
 * <pre>
 *     // Create a pre-signed request using one of the "presign" methods on S3Presigner
 *     PresignedRequest presignedRequest = ...;
 *
 *     // Create an SdkHttpClient using one of the implementations provided by the SDK
 *     SdkHttpClient httpClient = ApacheHttpClient.builder().build(); // or UrlConnectionHttpClient.create()
 *
 *     // Specify any request payload that is needed by the service (not needed when isBrowserExecutable is true)
 *     ContentStreamProvider requestPayload =
 *         presignedRequest.signedPayload()
 *                         .map(SdkBytes::asContentStreamProvider)
 *                         .orElse(null);
 *
 *     // Create the request for sending to the service
 *     HttpExecuteRequest request =
 *         HttpExecuteRequest.builder()
 *                           .request(presignedRequest.httpRequest())
 *                           .contentStreamProvider(requestPayload)
 *                           .build();
 *
 *     // Call the service
 *     HttpExecuteResponse response = httpClient.prepareRequest(request).call();
 *
 *     // Download the result of executing the request
 *     if (response.responseBody().isPresent()) {
 *         try (InputStream responseStream = response.responseBody().get()) {
 *             System.out.println("Service returned response: ");
 *             IoUtils.copy(content, System.out);
 *         }
 *     }
 * </pre>
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public interface S3Presigner extends SdkPresigner {
    /**
     * Create an {@link S3Presigner} with default configuration. The region will be loaded from the
     * {@link DefaultAwsRegionProviderChain} and credentials will be loaded from the {@link DefaultCredentialsProvider}.
     * <p/>
     * This is usually done at application startup, because creating a presigner can be expensive. It is recommended to
     * {@link #close()} the {@code S3Presigner} when it is done being used.
     */
    static S3Presigner create() {
        return builder().build();
    }

    /**
     * Create an {@link S3Presigner.Builder} that can be used to configure and create a {@link S3Presigner}.
     * <p/>
     * This is usually done at application startup, because creating a presigner can be expensive. It is recommended to
     * {@link #close()} the {@code S3Presigner} when it is done being used.
     */
    static Builder builder() {
        return DefaultS3Presigner.builder();
    }

    /**
     * Presign a {@link GetObjectRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p/>
     *
     * <b>Example Usage</b>
     * <p/>
     *
     * <pre>
     * {@code
     *     S3Presigner presigner = ...;
     *
     *     // Create a GetObjectRequest to be pre-signed
     *     GetObjectRequest getObjectRequest = ...;
     *
     *     // Create a GetObjectPresignRequest to specify the signature duration
     *     GetObjectPresignRequest getObjectPresignRequest =
     *         GetObjectPresignRequest.builder()
     *                                .signatureDuration(Duration.ofMinutes(10))
     *                                .getObjectRequest(request)
     *                                .build();
     *
     *     // Generate the presigned request
     *     PresignedGetObjectRequest presignedGetObjectRequest =
     *         presigner.presignGetObject(getObjectPresignRequest);
     *
     *     if (presignedGetObjectRequest.isBrowserExecutable())
     *         System.out.println("The pre-signed request can be executed using a web browser by " +
     *                            "visiting the following URL: " + presignedGetObjectRequest.url());
     *     else
     *         System.out.println("The pre-signed request has an HTTP method, headers or a payload " +
     *                            "that prohibits it from being executed by a web browser. See the S3Presigner " +
     *                            "class-level documentation for an example of how to execute this pre-signed " +
     *                            "request from Java code.");
     * }
     * </pre>
     */
    PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest request);

    /**
     * Presign a {@link GetObjectRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p />
     * This is a shorter method of invoking {@link #presignGetObject(GetObjectPresignRequest)} without needing
     * to call {@code GetObjectPresignRequest.builder()} or {@code .build()}.
     * 
     * @see #presignGetObject(GetObjectPresignRequest)
     */
    default PresignedGetObjectRequest presignGetObject(Consumer<GetObjectPresignRequest.Builder> request) {
        GetObjectPresignRequest.Builder builder = GetObjectPresignRequest.builder();
        request.accept(builder);
        return presignGetObject(builder.build());
    }

    /**
     * Presign a {@link PutObjectRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p/>
     *
     * <b>Example Usage</b>
     * <p/>
     *
     * <pre>
     * {@code
     *     S3Presigner presigner = ...;
     *
     *     // Create a PutObjectRequest to be pre-signed
     *     PutObjectRequest putObjectRequest = ...;
     *
     *     // Create a PutObjectPresignRequest to specify the signature duration
     *     PutObjectPresignRequest putObjectPresignRequest =
     *         PutObjectPresignRequest.builder()
     *                                .signatureDuration(Duration.ofMinutes(10))
     *                                .putObjectRequest(request)
     *                                .build();
     *
     *     // Generate the presigned request
     *     PresignedPutObjectRequest presignedPutObjectRequest =
     *         presigner.presignPutObject(putObjectPresignRequest);
     * }
     * </pre>
     */
    PresignedPutObjectRequest presignPutObject(PutObjectPresignRequest request);

    /**
     * Presign a {@link PutObjectRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p />
     * This is a shorter method of invoking {@link #presignPutObject(PutObjectPresignRequest)} without needing
     * to call {@code PutObjectPresignRequest.builder()} or {@code .build()}.
     *
     * @see #presignPutObject(PutObjectPresignRequest)
     */
    default PresignedPutObjectRequest presignPutObject(Consumer<PutObjectPresignRequest.Builder> request) {
        PutObjectPresignRequest.Builder builder = PutObjectPresignRequest.builder();
        request.accept(builder);
        return presignPutObject(builder.build());
    }

    /**
     * Presign a {@link CreateMultipartUploadRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p/>
     *
     * <b>Example Usage</b>
     * <p/>
     *
     * <pre>
     * {@code
     *     S3Presigner presigner = ...;
     *
     *     // Create a CreateMultipartUploadRequest to be pre-signed
     *     CreateMultipartUploadRequest createMultipartUploadRequest = ...;
     *
     *     // Create a CreateMultipartUploadPresignRequest to specify the signature duration
     *     CreateMultipartUploadPresignRequest createMultipartUploadPresignRequest =
     *         CreateMultipartUploadPresignRequest.builder()
     *                                            .signatureDuration(Duration.ofMinutes(10))
     *                                            .createMultipartUploadRequest(request)
     *                                            .build();
     *
     *     // Generate the presigned request
     *     PresignedCreateMultipartUploadRequest presignedCreateMultipartUploadRequest =
     *         presigner.presignCreateMultipartUpload(createMultipartUploadPresignRequest);
     * }
     * </pre>
     */
    PresignedCreateMultipartUploadRequest presignCreateMultipartUpload(CreateMultipartUploadPresignRequest request);

    /**
     * Presign a {@link CreateMultipartUploadRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p />
     * This is a shorter method of invoking {@link #presignCreateMultipartUpload(CreateMultipartUploadPresignRequest)} without
     * needing to call {@code CreateMultipartUploadPresignRequest.builder()} or {@code .build()}.
     *
     * @see #presignCreateMultipartUpload(CreateMultipartUploadPresignRequest)
     */
    default PresignedCreateMultipartUploadRequest presignCreateMultipartUpload(
        Consumer<CreateMultipartUploadPresignRequest.Builder> request) {
        CreateMultipartUploadPresignRequest.Builder builder = CreateMultipartUploadPresignRequest.builder();
        request.accept(builder);
        return presignCreateMultipartUpload(builder.build());
    }

    /**
     * Presign a {@link UploadPartRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p/>
     *
     * <b>Example Usage</b>
     * <p/>
     *
     * <pre>
     * {@code
     *     S3Presigner presigner = ...;
     *
     *     // Create a UploadPartRequest to be pre-signed
     *     UploadPartRequest uploadPartRequest = ...;
     *
     *     // Create a UploadPartPresignRequest to specify the signature duration
     *     UploadPartPresignRequest uploadPartPresignRequest =
     *         UploadPartPresignRequest.builder()
     *                                 .signatureDuration(Duration.ofMinutes(10))
     *                                 .uploadPartRequest(request)
     *                                 .build();
     *
     *     // Generate the presigned request
     *     PresignedUploadPartRequest presignedUploadPartRequest =
     *         presigner.presignUploadPart(uploadPartPresignRequest);
     * }
     * </pre>
     */
    PresignedUploadPartRequest presignUploadPart(UploadPartPresignRequest request);

    /**
     * Presign a {@link UploadPartRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p />
     * This is a shorter method of invoking {@link #presignUploadPart(UploadPartPresignRequest)} without needing
     * to call {@code UploadPartPresignRequest.builder()} or {@code .build()}.
     *
     * @see #presignUploadPart(UploadPartPresignRequest)
     */
    default PresignedUploadPartRequest presignUploadPart(Consumer<UploadPartPresignRequest.Builder> request) {
        UploadPartPresignRequest.Builder builder = UploadPartPresignRequest.builder();
        request.accept(builder);
        return presignUploadPart(builder.build());
    }

    /**
     * Presign a {@link CompleteMultipartUploadRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p/>
     *
     * <b>Example Usage</b>
     * <p/>
     *
     * <pre>
     * {@code
     *     S3Presigner presigner = ...;
     *
     *     // Complete a CompleteMultipartUploadRequest to be pre-signed
     *     CompleteMultipartUploadRequest completeMultipartUploadRequest = ...;
     *
     *     // Create a CompleteMultipartUploadPresignRequest to specify the signature duration
     *     CompleteMultipartUploadPresignRequest completeMultipartUploadPresignRequest =
     *         CompleteMultipartUploadPresignRequest.builder()
     *                                              .signatureDuration(Duration.ofMinutes(10))
     *                                              .completeMultipartUploadRequest(request)
     *                                              .build();
     *
     *     // Generate the presigned request
     *     PresignedCompleteMultipartUploadRequest presignedCompleteMultipartUploadRequest =
     *         presigner.presignCompleteMultipartUpload(completeMultipartUploadPresignRequest);
     * }
     * </pre>
     */
    PresignedCompleteMultipartUploadRequest presignCompleteMultipartUpload(CompleteMultipartUploadPresignRequest request);

    /**
     * Presign a {@link CompleteMultipartUploadRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p />
     * This is a shorter method of invoking {@link #presignCompleteMultipartUpload(CompleteMultipartUploadPresignRequest)} without
     * needing to call {@code CompleteMultipartUploadPresignRequest.builder()} or {@code .build()}.
     *
     * @see #presignCompleteMultipartUpload(CompleteMultipartUploadPresignRequest)
     */
    default PresignedCompleteMultipartUploadRequest presignCompleteMultipartUpload(
        Consumer<CompleteMultipartUploadPresignRequest.Builder> request) {
        CompleteMultipartUploadPresignRequest.Builder builder = CompleteMultipartUploadPresignRequest.builder();
        request.accept(builder);
        return presignCompleteMultipartUpload(builder.build());
    }

    /**
     * Presign a {@link AbortMultipartUploadRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p/>
     *
     * <b>Example Usage</b>
     * <p/>
     *
     * <pre>
     * {@code
     *     S3Presigner presigner = ...;
     *
     *     // Complete a AbortMultipartUploadRequest to be pre-signed
     *     AbortMultipartUploadRequest abortMultipartUploadRequest = ...;
     *
     *     // Create a AbortMultipartUploadPresignRequest to specify the signature duration
     *     AbortMultipartUploadPresignRequest abortMultipartUploadPresignRequest =
     *         AbortMultipartUploadPresignRequest.builder()
     *                                              .signatureDuration(Duration.ofMinutes(10))
     *                                              .abortMultipartUploadRequest(request)
     *                                              .build();
     *
     *     // Generate the presigned request
     *     PresignedAbortMultipartUploadRequest presignedAbortMultipartUploadRequest =
     *         presigner.presignAbortMultipartUpload(abortMultipartUploadPresignRequest);
     * }
     * </pre>
     */
    PresignedAbortMultipartUploadRequest presignAbortMultipartUpload(AbortMultipartUploadPresignRequest request);

    /**
     * Presign a {@link AbortMultipartUploadRequest} so that it can be executed at a later time without requiring additional
     * signing or authentication.
     * <p />
     * This is a shorter method of invoking {@link #presignAbortMultipartUpload(AbortMultipartUploadPresignRequest)} without
     * needing to call {@code AbortMultipartUploadPresignRequest.builder()} or {@code .build()}.
     *
     * @see #presignAbortMultipartUpload(AbortMultipartUploadPresignRequest)
     */
    default PresignedAbortMultipartUploadRequest presignAbortMultipartUpload(
        Consumer<AbortMultipartUploadPresignRequest.Builder> request) {
        AbortMultipartUploadPresignRequest.Builder builder = AbortMultipartUploadPresignRequest.builder();
        request.accept(builder);
        return presignAbortMultipartUpload(builder.build());
    }

    /**
     * A builder for creating {@link S3Presigner}s. Created using {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    interface Builder extends SdkPresigner.Builder {
        /**
         * Allows providing a custom S3 serviceConfiguration by providing a {@link S3Configuration} object;
         *
         * Note: chunkedEncodingEnabled and checksumValidationEnabled do not apply to presigned requests.
         *
         * @param serviceConfiguration {@link S3Configuration}
         * @return this Builder
         */
        Builder serviceConfiguration(S3Configuration serviceConfiguration);

        @Override
        Builder region(Region region);

        @Override
        Builder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        @Override
        Builder endpointOverride(URI endpointOverride);

        @Override
        S3Presigner build();
    }
}

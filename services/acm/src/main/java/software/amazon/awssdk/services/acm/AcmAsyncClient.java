/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.services.acm.model.AddTagsToCertificateRequest;
import software.amazon.awssdk.services.acm.model.AddTagsToCertificateResponse;
import software.amazon.awssdk.services.acm.model.DeleteCertificateRequest;
import software.amazon.awssdk.services.acm.model.DeleteCertificateResponse;
import software.amazon.awssdk.services.acm.model.DescribeCertificateRequest;
import software.amazon.awssdk.services.acm.model.DescribeCertificateResponse;
import software.amazon.awssdk.services.acm.model.ExportCertificateRequest;
import software.amazon.awssdk.services.acm.model.ExportCertificateResponse;
import software.amazon.awssdk.services.acm.model.GetAccountConfigurationRequest;
import software.amazon.awssdk.services.acm.model.GetAccountConfigurationResponse;
import software.amazon.awssdk.services.acm.model.GetCertificateRequest;
import software.amazon.awssdk.services.acm.model.GetCertificateResponse;
import software.amazon.awssdk.services.acm.model.ImportCertificateRequest;
import software.amazon.awssdk.services.acm.model.ImportCertificateResponse;
import software.amazon.awssdk.services.acm.model.ListCertificatesRequest;
import software.amazon.awssdk.services.acm.model.ListCertificatesResponse;
import software.amazon.awssdk.services.acm.model.ListTagsForCertificateRequest;
import software.amazon.awssdk.services.acm.model.ListTagsForCertificateResponse;
import software.amazon.awssdk.services.acm.model.PutAccountConfigurationRequest;
import software.amazon.awssdk.services.acm.model.PutAccountConfigurationResponse;
import software.amazon.awssdk.services.acm.model.RemoveTagsFromCertificateRequest;
import software.amazon.awssdk.services.acm.model.RemoveTagsFromCertificateResponse;
import software.amazon.awssdk.services.acm.model.RenewCertificateRequest;
import software.amazon.awssdk.services.acm.model.RenewCertificateResponse;
import software.amazon.awssdk.services.acm.model.RequestCertificateRequest;
import software.amazon.awssdk.services.acm.model.RequestCertificateResponse;
import software.amazon.awssdk.services.acm.model.ResendValidationEmailRequest;
import software.amazon.awssdk.services.acm.model.ResendValidationEmailResponse;
import software.amazon.awssdk.services.acm.model.UpdateCertificateOptionsRequest;
import software.amazon.awssdk.services.acm.model.UpdateCertificateOptionsResponse;
import software.amazon.awssdk.services.acm.paginators.ListCertificatesPublisher;
import software.amazon.awssdk.services.acm.waiters.AcmAsyncWaiter;

/**
 * Service client for accessing ACM asynchronously. This can be created using the static {@link #builder()} method.
 *
 * <fullname>Certificate Manager</fullname>
 * <p>
 * You can use Certificate Manager (ACM) to manage SSL/TLS certificates for your Amazon Web Services-based websites and
 * applications. For more information about using ACM, see the <a
 * href="https://docs.aws.amazon.com/acm/latest/userguide/">Certificate Manager User Guide</a>.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface AcmAsyncClient extends AwsClient {
    String SERVICE_NAME = "acm";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "acm";

    /**
     * <p>
     * Adds one or more tags to an ACM certificate. Tags are labels that you can use to identify and organize your
     * Amazon Web Services resources. Each tag consists of a <code>key</code> and an optional <code>value</code>. You
     * specify the certificate on input by its Amazon Resource Name (ARN). You specify the tag by using a key-value
     * pair.
     * </p>
     * <p>
     * You can apply a tag to just one certificate if you want to identify a specific characteristic of that
     * certificate, or you can apply the same tag to multiple certificates if you want to filter for a common
     * relationship among those certificates. Similarly, you can apply the same tag to multiple resources if you want to
     * specify a relationship among those resources. For example, you can add the same tag to an ACM certificate and an
     * Elastic Load Balancing load balancer to indicate that they are both used by the same website. For more
     * information, see <a href="https://docs.aws.amazon.com/acm/latest/userguide/tags.html">Tagging ACM
     * certificates</a>.
     * </p>
     * <p>
     * To remove one or more tags, use the <a>RemoveTagsFromCertificate</a> action. To view all of the tags that have
     * been applied to the certificate, use the <a>ListTagsForCertificate</a> action.
     * </p>
     *
     * @param addTagsToCertificateRequest
     * @return A Java Future containing the result of the AddTagsToCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>InvalidTagException One or both of the values that make up the key-value pair is not valid. For
     *         example, you cannot specify a tag value that begins with <code>aws:</code>.</li>
     *         <li>TooManyTagsException The request contains too many tags. Try the request again with fewer tags.</li>
     *         <li>TagPolicyException A specified tag did not comply with an existing tag policy and was rejected.</li>
     *         <li>InvalidParameterException An input parameter was invalid.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.AddTagsToCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/AddTagsToCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<AddTagsToCertificateResponse> addTagsToCertificate(
            AddTagsToCertificateRequest addTagsToCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Adds one or more tags to an ACM certificate. Tags are labels that you can use to identify and organize your
     * Amazon Web Services resources. Each tag consists of a <code>key</code> and an optional <code>value</code>. You
     * specify the certificate on input by its Amazon Resource Name (ARN). You specify the tag by using a key-value
     * pair.
     * </p>
     * <p>
     * You can apply a tag to just one certificate if you want to identify a specific characteristic of that
     * certificate, or you can apply the same tag to multiple certificates if you want to filter for a common
     * relationship among those certificates. Similarly, you can apply the same tag to multiple resources if you want to
     * specify a relationship among those resources. For example, you can add the same tag to an ACM certificate and an
     * Elastic Load Balancing load balancer to indicate that they are both used by the same website. For more
     * information, see <a href="https://docs.aws.amazon.com/acm/latest/userguide/tags.html">Tagging ACM
     * certificates</a>.
     * </p>
     * <p>
     * To remove one or more tags, use the <a>RemoveTagsFromCertificate</a> action. To view all of the tags that have
     * been applied to the certificate, use the <a>ListTagsForCertificate</a> action.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link AddTagsToCertificateRequest.Builder} avoiding the
     * need to create one manually via {@link AddTagsToCertificateRequest#builder()}
     * </p>
     *
     * @param addTagsToCertificateRequest
     *        A {@link Consumer} that will call methods on {@link AddTagsToCertificateRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the AddTagsToCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>InvalidTagException One or both of the values that make up the key-value pair is not valid. For
     *         example, you cannot specify a tag value that begins with <code>aws:</code>.</li>
     *         <li>TooManyTagsException The request contains too many tags. Try the request again with fewer tags.</li>
     *         <li>TagPolicyException A specified tag did not comply with an existing tag policy and was rejected.</li>
     *         <li>InvalidParameterException An input parameter was invalid.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.AddTagsToCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/AddTagsToCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<AddTagsToCertificateResponse> addTagsToCertificate(
            Consumer<AddTagsToCertificateRequest.Builder> addTagsToCertificateRequest) {
        return addTagsToCertificate(AddTagsToCertificateRequest.builder().applyMutation(addTagsToCertificateRequest).build());
    }

    /**
     * <p>
     * Deletes a certificate and its associated private key. If this action succeeds, the certificate no longer appears
     * in the list that can be displayed by calling the <a>ListCertificates</a> action or be retrieved by calling the
     * <a>GetCertificate</a> action. The certificate will not be available for use by Amazon Web Services services
     * integrated with ACM.
     * </p>
     * <note>
     * <p>
     * You cannot delete an ACM certificate that is being used by another Amazon Web Services service. To delete a
     * certificate that is in use, the certificate association must first be removed.
     * </p>
     * </note>
     *
     * @param deleteCertificateRequest
     * @return A Java Future containing the result of the DeleteCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>ResourceInUseException The certificate is in use by another Amazon Web Services service in the
     *         caller's account. Remove the association and try again.</li>
     *         <li>AccessDeniedException You do not have access required to perform this action.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>ConflictException You are trying to update a resource or configuration that is already being created
     *         or updated. Wait for the previous operation to finish and try again.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.DeleteCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/DeleteCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DeleteCertificateResponse> deleteCertificate(DeleteCertificateRequest deleteCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Deletes a certificate and its associated private key. If this action succeeds, the certificate no longer appears
     * in the list that can be displayed by calling the <a>ListCertificates</a> action or be retrieved by calling the
     * <a>GetCertificate</a> action. The certificate will not be available for use by Amazon Web Services services
     * integrated with ACM.
     * </p>
     * <note>
     * <p>
     * You cannot delete an ACM certificate that is being used by another Amazon Web Services service. To delete a
     * certificate that is in use, the certificate association must first be removed.
     * </p>
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteCertificateRequest.Builder} avoiding the need
     * to create one manually via {@link DeleteCertificateRequest#builder()}
     * </p>
     *
     * @param deleteCertificateRequest
     *        A {@link Consumer} that will call methods on {@link DeleteCertificateRequest.Builder} to create a request.
     * @return A Java Future containing the result of the DeleteCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>ResourceInUseException The certificate is in use by another Amazon Web Services service in the
     *         caller's account. Remove the association and try again.</li>
     *         <li>AccessDeniedException You do not have access required to perform this action.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>ConflictException You are trying to update a resource or configuration that is already being created
     *         or updated. Wait for the previous operation to finish and try again.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.DeleteCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/DeleteCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DeleteCertificateResponse> deleteCertificate(
            Consumer<DeleteCertificateRequest.Builder> deleteCertificateRequest) {
        return deleteCertificate(DeleteCertificateRequest.builder().applyMutation(deleteCertificateRequest).build());
    }

    /**
     * <p>
     * Returns detailed metadata about the specified ACM certificate.
     * </p>
     * <p>
     * If you have just created a certificate using the <code>RequestCertificate</code> action, there is a delay of
     * several seconds before you can retrieve information about it.
     * </p>
     *
     * @param describeCertificateRequest
     * @return A Java Future containing the result of the DescribeCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.DescribeCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/DescribeCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DescribeCertificateResponse> describeCertificate(
            DescribeCertificateRequest describeCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Returns detailed metadata about the specified ACM certificate.
     * </p>
     * <p>
     * If you have just created a certificate using the <code>RequestCertificate</code> action, there is a delay of
     * several seconds before you can retrieve information about it.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DescribeCertificateRequest.Builder} avoiding the
     * need to create one manually via {@link DescribeCertificateRequest#builder()}
     * </p>
     *
     * @param describeCertificateRequest
     *        A {@link Consumer} that will call methods on {@link DescribeCertificateRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the DescribeCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.DescribeCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/DescribeCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<DescribeCertificateResponse> describeCertificate(
            Consumer<DescribeCertificateRequest.Builder> describeCertificateRequest) {
        return describeCertificate(DescribeCertificateRequest.builder().applyMutation(describeCertificateRequest).build());
    }

    /**
     * <p>
     * Exports a private certificate issued by a private certificate authority (CA) for use anywhere. The exported file
     * contains the certificate, the certificate chain, and the encrypted private 2048-bit RSA key associated with the
     * public key that is embedded in the certificate. For security, you must assign a passphrase for the private key
     * when exporting it.
     * </p>
     * <p>
     * For information about exporting and formatting a certificate using the ACM console or CLI, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-export-private.html">Export a Private
     * Certificate</a>.
     * </p>
     *
     * @param exportCertificateRequest
     * @return A Java Future containing the result of the ExportCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>RequestInProgressException The certificate request is in process and the certificate in your account
     *         has not yet been issued.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ExportCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ExportCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ExportCertificateResponse> exportCertificate(ExportCertificateRequest exportCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Exports a private certificate issued by a private certificate authority (CA) for use anywhere. The exported file
     * contains the certificate, the certificate chain, and the encrypted private 2048-bit RSA key associated with the
     * public key that is embedded in the certificate. For security, you must assign a passphrase for the private key
     * when exporting it.
     * </p>
     * <p>
     * For information about exporting and formatting a certificate using the ACM console or CLI, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-export-private.html">Export a Private
     * Certificate</a>.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ExportCertificateRequest.Builder} avoiding the need
     * to create one manually via {@link ExportCertificateRequest#builder()}
     * </p>
     *
     * @param exportCertificateRequest
     *        A {@link Consumer} that will call methods on {@link ExportCertificateRequest.Builder} to create a request.
     * @return A Java Future containing the result of the ExportCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>RequestInProgressException The certificate request is in process and the certificate in your account
     *         has not yet been issued.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ExportCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ExportCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ExportCertificateResponse> exportCertificate(
            Consumer<ExportCertificateRequest.Builder> exportCertificateRequest) {
        return exportCertificate(ExportCertificateRequest.builder().applyMutation(exportCertificateRequest).build());
    }

    /**
     * <p>
     * Returns the account configuration options associated with an Amazon Web Services account.
     * </p>
     *
     * @param getAccountConfigurationRequest
     * @return A Java Future containing the result of the GetAccountConfiguration operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>AccessDeniedException You do not have access required to perform this action.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.GetAccountConfiguration
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/GetAccountConfiguration" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<GetAccountConfigurationResponse> getAccountConfiguration(
            GetAccountConfigurationRequest getAccountConfigurationRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Returns the account configuration options associated with an Amazon Web Services account.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetAccountConfigurationRequest.Builder} avoiding
     * the need to create one manually via {@link GetAccountConfigurationRequest#builder()}
     * </p>
     *
     * @param getAccountConfigurationRequest
     *        A {@link Consumer} that will call methods on {@link GetAccountConfigurationRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the GetAccountConfiguration operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>AccessDeniedException You do not have access required to perform this action.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.GetAccountConfiguration
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/GetAccountConfiguration" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<GetAccountConfigurationResponse> getAccountConfiguration(
            Consumer<GetAccountConfigurationRequest.Builder> getAccountConfigurationRequest) {
        return getAccountConfiguration(GetAccountConfigurationRequest.builder().applyMutation(getAccountConfigurationRequest)
                .build());
    }

    /**
     * <p>
     * Retrieves an Amazon-issued certificate and its certificate chain. The chain consists of the certificate of the
     * issuing CA and the intermediate certificates of any other subordinate CAs. All of the certificates are base64
     * encoded. You can use <a href="https://wiki.openssl.org/index.php/Command_Line_Utilities">OpenSSL</a> to decode
     * the certificates and inspect individual fields.
     * </p>
     *
     * @param getCertificateRequest
     * @return A Java Future containing the result of the GetCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>RequestInProgressException The certificate request is in process and the certificate in your account
     *         has not yet been issued.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.GetCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/GetCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<GetCertificateResponse> getCertificate(GetCertificateRequest getCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves an Amazon-issued certificate and its certificate chain. The chain consists of the certificate of the
     * issuing CA and the intermediate certificates of any other subordinate CAs. All of the certificates are base64
     * encoded. You can use <a href="https://wiki.openssl.org/index.php/Command_Line_Utilities">OpenSSL</a> to decode
     * the certificates and inspect individual fields.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetCertificateRequest.Builder} avoiding the need to
     * create one manually via {@link GetCertificateRequest#builder()}
     * </p>
     *
     * @param getCertificateRequest
     *        A {@link Consumer} that will call methods on {@link GetCertificateRequest.Builder} to create a request.
     * @return A Java Future containing the result of the GetCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>RequestInProgressException The certificate request is in process and the certificate in your account
     *         has not yet been issued.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.GetCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/GetCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<GetCertificateResponse> getCertificate(Consumer<GetCertificateRequest.Builder> getCertificateRequest) {
        return getCertificate(GetCertificateRequest.builder().applyMutation(getCertificateRequest).build());
    }

    /**
     * <p>
     * Imports a certificate into Certificate Manager (ACM) to use with services that are integrated with ACM. Note that
     * <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-services.html">integrated services</a> allow only
     * certificate types and keys they support to be associated with their resources. Further, their support differs
     * depending on whether the certificate is imported into IAM or into ACM. For more information, see the
     * documentation for each service. For more information about importing certificates into ACM, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing Certificates</a> in the
     * <i>Certificate Manager User Guide</i>.
     * </p>
     * <note>
     * <p>
     * ACM does not provide <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
     * renewal</a> for certificates that you import.
     * </p>
     * </note>
     * <p>
     * Note the following guidelines when importing third party certificates:
     * </p>
     * <ul>
     * <li>
     * <p>
     * You must enter the private key that matches the certificate you are importing.
     * </p>
     * </li>
     * <li>
     * <p>
     * The private key must be unencrypted. You cannot import a private key that is protected by a password or a
     * passphrase.
     * </p>
     * </li>
     * <li>
     * <p>
     * The private key must be no larger than 5 KB (5,120 bytes).
     * </p>
     * </li>
     * <li>
     * <p>
     * If the certificate you are importing is not self-signed, you must enter its certificate chain.
     * </p>
     * </li>
     * <li>
     * <p>
     * If a certificate chain is included, the issuer must be the subject of one of the certificates in the chain.
     * </p>
     * </li>
     * <li>
     * <p>
     * The certificate, private key, and certificate chain must be PEM-encoded.
     * </p>
     * </li>
     * <li>
     * <p>
     * The current time must be between the <code>Not Before</code> and <code>Not After</code> certificate fields.
     * </p>
     * </li>
     * <li>
     * <p>
     * The <code>Issuer</code> field must not be empty.
     * </p>
     * </li>
     * <li>
     * <p>
     * The OCSP authority URL, if present, must not exceed 1000 characters.
     * </p>
     * </li>
     * <li>
     * <p>
     * To import a new certificate, omit the <code>CertificateArn</code> argument. Include this argument only when you
     * want to replace a previously imported certificate.
     * </p>
     * </li>
     * <li>
     * <p>
     * When you import a certificate by using the CLI, you must specify the certificate, the certificate chain, and the
     * private key by their file names preceded by <code>fileb://</code>. For example, you can specify a certificate
     * saved in the <code>C:\temp</code> folder as <code>fileb://C:\temp\certificate_to_import.pem</code>. If you are
     * making an HTTP or HTTPS Query request, include these arguments as BLOBs.
     * </p>
     * </li>
     * <li>
     * <p>
     * When you import a certificate by using an SDK, you must specify the certificate, the certificate chain, and the
     * private key files in the manner required by the programming language you're using.
     * </p>
     * </li>
     * <li>
     * <p>
     * The cryptographic algorithm of an imported certificate must match the algorithm of the signing CA. For example,
     * if the signing CA key type is RSA, then the certificate key type must also be RSA.
     * </p>
     * </li>
     * </ul>
     * <p>
     * This operation returns the <a
     * href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Name (ARN)</a>
     * of the imported certificate.
     * </p>
     *
     * @param importCertificateRequest
     * @return A Java Future containing the result of the ImportCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>LimitExceededException An ACM quota has been exceeded.</li>
     *         <li>InvalidTagException One or both of the values that make up the key-value pair is not valid. For
     *         example, you cannot specify a tag value that begins with <code>aws:</code>.</li>
     *         <li>TooManyTagsException The request contains too many tags. Try the request again with fewer tags.</li>
     *         <li>TagPolicyException A specified tag did not comply with an existing tag policy and was rejected.</li>
     *         <li>InvalidParameterException An input parameter was invalid.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ImportCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ImportCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ImportCertificateResponse> importCertificate(ImportCertificateRequest importCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Imports a certificate into Certificate Manager (ACM) to use with services that are integrated with ACM. Note that
     * <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-services.html">integrated services</a> allow only
     * certificate types and keys they support to be associated with their resources. Further, their support differs
     * depending on whether the certificate is imported into IAM or into ACM. For more information, see the
     * documentation for each service. For more information about importing certificates into ACM, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing Certificates</a> in the
     * <i>Certificate Manager User Guide</i>.
     * </p>
     * <note>
     * <p>
     * ACM does not provide <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
     * renewal</a> for certificates that you import.
     * </p>
     * </note>
     * <p>
     * Note the following guidelines when importing third party certificates:
     * </p>
     * <ul>
     * <li>
     * <p>
     * You must enter the private key that matches the certificate you are importing.
     * </p>
     * </li>
     * <li>
     * <p>
     * The private key must be unencrypted. You cannot import a private key that is protected by a password or a
     * passphrase.
     * </p>
     * </li>
     * <li>
     * <p>
     * The private key must be no larger than 5 KB (5,120 bytes).
     * </p>
     * </li>
     * <li>
     * <p>
     * If the certificate you are importing is not self-signed, you must enter its certificate chain.
     * </p>
     * </li>
     * <li>
     * <p>
     * If a certificate chain is included, the issuer must be the subject of one of the certificates in the chain.
     * </p>
     * </li>
     * <li>
     * <p>
     * The certificate, private key, and certificate chain must be PEM-encoded.
     * </p>
     * </li>
     * <li>
     * <p>
     * The current time must be between the <code>Not Before</code> and <code>Not After</code> certificate fields.
     * </p>
     * </li>
     * <li>
     * <p>
     * The <code>Issuer</code> field must not be empty.
     * </p>
     * </li>
     * <li>
     * <p>
     * The OCSP authority URL, if present, must not exceed 1000 characters.
     * </p>
     * </li>
     * <li>
     * <p>
     * To import a new certificate, omit the <code>CertificateArn</code> argument. Include this argument only when you
     * want to replace a previously imported certificate.
     * </p>
     * </li>
     * <li>
     * <p>
     * When you import a certificate by using the CLI, you must specify the certificate, the certificate chain, and the
     * private key by their file names preceded by <code>fileb://</code>. For example, you can specify a certificate
     * saved in the <code>C:\temp</code> folder as <code>fileb://C:\temp\certificate_to_import.pem</code>. If you are
     * making an HTTP or HTTPS Query request, include these arguments as BLOBs.
     * </p>
     * </li>
     * <li>
     * <p>
     * When you import a certificate by using an SDK, you must specify the certificate, the certificate chain, and the
     * private key files in the manner required by the programming language you're using.
     * </p>
     * </li>
     * <li>
     * <p>
     * The cryptographic algorithm of an imported certificate must match the algorithm of the signing CA. For example,
     * if the signing CA key type is RSA, then the certificate key type must also be RSA.
     * </p>
     * </li>
     * </ul>
     * <p>
     * This operation returns the <a
     * href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Name (ARN)</a>
     * of the imported certificate.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ImportCertificateRequest.Builder} avoiding the need
     * to create one manually via {@link ImportCertificateRequest#builder()}
     * </p>
     *
     * @param importCertificateRequest
     *        A {@link Consumer} that will call methods on {@link ImportCertificateRequest.Builder} to create a request.
     * @return A Java Future containing the result of the ImportCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>LimitExceededException An ACM quota has been exceeded.</li>
     *         <li>InvalidTagException One or both of the values that make up the key-value pair is not valid. For
     *         example, you cannot specify a tag value that begins with <code>aws:</code>.</li>
     *         <li>TooManyTagsException The request contains too many tags. Try the request again with fewer tags.</li>
     *         <li>TagPolicyException A specified tag did not comply with an existing tag policy and was rejected.</li>
     *         <li>InvalidParameterException An input parameter was invalid.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ImportCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ImportCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ImportCertificateResponse> importCertificate(
            Consumer<ImportCertificateRequest.Builder> importCertificateRequest) {
        return importCertificate(ImportCertificateRequest.builder().applyMutation(importCertificateRequest).build());
    }

    /**
     * <p>
     * Retrieves a list of certificate ARNs and domain names. You can request that only certificates that match a
     * specific status be listed. You can also filter by specific attributes of the certificate. Default filtering
     * returns only <code>RSA_2048</code> certificates. For more information, see <a>Filters</a>.
     * </p>
     *
     * @param listCertificatesRequest
     * @return A Java Future containing the result of the ListCertificates operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgsException One or more of of request parameters specified is not valid.</li>
     *         <li>ValidationException The supplied input failed to satisfy constraints of an Amazon Web Services
     *         service.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ListCertificates
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ListCertificates" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListCertificatesResponse> listCertificates(ListCertificatesRequest listCertificatesRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves a list of certificate ARNs and domain names. You can request that only certificates that match a
     * specific status be listed. You can also filter by specific attributes of the certificate. Default filtering
     * returns only <code>RSA_2048</code> certificates. For more information, see <a>Filters</a>.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListCertificatesRequest.Builder} avoiding the need
     * to create one manually via {@link ListCertificatesRequest#builder()}
     * </p>
     *
     * @param listCertificatesRequest
     *        A {@link Consumer} that will call methods on {@link ListCertificatesRequest.Builder} to create a request.
     * @return A Java Future containing the result of the ListCertificates operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgsException One or more of of request parameters specified is not valid.</li>
     *         <li>ValidationException The supplied input failed to satisfy constraints of an Amazon Web Services
     *         service.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ListCertificates
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ListCertificates" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListCertificatesResponse> listCertificates(
            Consumer<ListCertificatesRequest.Builder> listCertificatesRequest) {
        return listCertificates(ListCertificatesRequest.builder().applyMutation(listCertificatesRequest).build());
    }

    /**
     * <p>
     * Retrieves a list of certificate ARNs and domain names. You can request that only certificates that match a
     * specific status be listed. You can also filter by specific attributes of the certificate. Default filtering
     * returns only <code>RSA_2048</code> certificates. For more information, see <a>Filters</a>.
     * </p>
     *
     * @return A Java Future containing the result of the ListCertificates operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgsException One or more of of request parameters specified is not valid.</li>
     *         <li>ValidationException The supplied input failed to satisfy constraints of an Amazon Web Services
     *         service.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ListCertificates
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ListCertificates" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ListCertificatesResponse> listCertificates() {
        return listCertificates(ListCertificatesRequest.builder().build());
    }

    /**
     * <p>
     * Retrieves a list of certificate ARNs and domain names. You can request that only certificates that match a
     * specific status be listed. You can also filter by specific attributes of the certificate. Default filtering
     * returns only <code>RSA_2048</code> certificates. For more information, see <a>Filters</a>.
     * </p>
     * <br/>
     * <p>
     * This is a variant of {@link #listCertificates(software.amazon.awssdk.services.acm.model.ListCertificatesRequest)}
     * operation. The return type is a custom publisher that can be subscribed to request a stream of response pages.
     * SDK will internally handle making service calls for you.
     * </p>
     * <p>
     * When the operation is called, an instance of this class is returned. At this point, no service calls are made yet
     * and so there is no guarantee that the request is valid. If there are errors in your request, you will see the
     * failures only after you start streaming the data. The subscribe method should be called as a request to start
     * streaming data. For more info, see
     * {@link org.reactivestreams.Publisher#subscribe(org.reactivestreams.Subscriber)}. Each call to the subscribe
     * method will result in a new {@link org.reactivestreams.Subscription} i.e., a new contract to stream data from the
     * starting request.
     * </p>
     *
     * <p>
     * The following are few ways to use the response class:
     * </p>
     * 1) Using the subscribe helper method
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.acm.paginators.ListCertificatesPublisher publisher = client.listCertificatesPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.acm.paginators.ListCertificatesPublisher publisher = client.listCertificatesPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.acm.model.ListCertificatesResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.acm.model.ListCertificatesResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of MaxItems won't limit the number of results you get with the paginator.
     * It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listCertificates(software.amazon.awssdk.services.acm.model.ListCertificatesRequest)} operation.</b>
     * </p>
     *
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgsException One or more of of request parameters specified is not valid.</li>
     *         <li>ValidationException The supplied input failed to satisfy constraints of an Amazon Web Services
     *         service.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ListCertificates
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ListCertificates" target="_top">AWS API
     *      Documentation</a>
     */
    default ListCertificatesPublisher listCertificatesPaginator() {
        return listCertificatesPaginator(ListCertificatesRequest.builder().build());
    }

    /**
     * <p>
     * Retrieves a list of certificate ARNs and domain names. You can request that only certificates that match a
     * specific status be listed. You can also filter by specific attributes of the certificate. Default filtering
     * returns only <code>RSA_2048</code> certificates. For more information, see <a>Filters</a>.
     * </p>
     * <br/>
     * <p>
     * This is a variant of {@link #listCertificates(software.amazon.awssdk.services.acm.model.ListCertificatesRequest)}
     * operation. The return type is a custom publisher that can be subscribed to request a stream of response pages.
     * SDK will internally handle making service calls for you.
     * </p>
     * <p>
     * When the operation is called, an instance of this class is returned. At this point, no service calls are made yet
     * and so there is no guarantee that the request is valid. If there are errors in your request, you will see the
     * failures only after you start streaming the data. The subscribe method should be called as a request to start
     * streaming data. For more info, see
     * {@link org.reactivestreams.Publisher#subscribe(org.reactivestreams.Subscriber)}. Each call to the subscribe
     * method will result in a new {@link org.reactivestreams.Subscription} i.e., a new contract to stream data from the
     * starting request.
     * </p>
     *
     * <p>
     * The following are few ways to use the response class:
     * </p>
     * 1) Using the subscribe helper method
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.acm.paginators.ListCertificatesPublisher publisher = client.listCertificatesPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.acm.paginators.ListCertificatesPublisher publisher = client.listCertificatesPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.acm.model.ListCertificatesResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.acm.model.ListCertificatesResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of MaxItems won't limit the number of results you get with the paginator.
     * It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listCertificates(software.amazon.awssdk.services.acm.model.ListCertificatesRequest)} operation.</b>
     * </p>
     *
     * @param listCertificatesRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgsException One or more of of request parameters specified is not valid.</li>
     *         <li>ValidationException The supplied input failed to satisfy constraints of an Amazon Web Services
     *         service.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ListCertificates
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ListCertificates" target="_top">AWS API
     *      Documentation</a>
     */
    default ListCertificatesPublisher listCertificatesPaginator(ListCertificatesRequest listCertificatesRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves a list of certificate ARNs and domain names. You can request that only certificates that match a
     * specific status be listed. You can also filter by specific attributes of the certificate. Default filtering
     * returns only <code>RSA_2048</code> certificates. For more information, see <a>Filters</a>.
     * </p>
     * <br/>
     * <p>
     * This is a variant of {@link #listCertificates(software.amazon.awssdk.services.acm.model.ListCertificatesRequest)}
     * operation. The return type is a custom publisher that can be subscribed to request a stream of response pages.
     * SDK will internally handle making service calls for you.
     * </p>
     * <p>
     * When the operation is called, an instance of this class is returned. At this point, no service calls are made yet
     * and so there is no guarantee that the request is valid. If there are errors in your request, you will see the
     * failures only after you start streaming the data. The subscribe method should be called as a request to start
     * streaming data. For more info, see
     * {@link org.reactivestreams.Publisher#subscribe(org.reactivestreams.Subscriber)}. Each call to the subscribe
     * method will result in a new {@link org.reactivestreams.Subscription} i.e., a new contract to stream data from the
     * starting request.
     * </p>
     *
     * <p>
     * The following are few ways to use the response class:
     * </p>
     * 1) Using the subscribe helper method
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.acm.paginators.ListCertificatesPublisher publisher = client.listCertificatesPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.acm.paginators.ListCertificatesPublisher publisher = client.listCertificatesPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.acm.model.ListCertificatesResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.acm.model.ListCertificatesResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of MaxItems won't limit the number of results you get with the paginator.
     * It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listCertificates(software.amazon.awssdk.services.acm.model.ListCertificatesRequest)} operation.</b>
     * </p>
     * <p>
     * This is a convenience which creates an instance of the {@link ListCertificatesRequest.Builder} avoiding the need
     * to create one manually via {@link ListCertificatesRequest#builder()}
     * </p>
     *
     * @param listCertificatesRequest
     *        A {@link Consumer} that will call methods on {@link ListCertificatesRequest.Builder} to create a request.
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidArgsException One or more of of request parameters specified is not valid.</li>
     *         <li>ValidationException The supplied input failed to satisfy constraints of an Amazon Web Services
     *         service.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ListCertificates
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ListCertificates" target="_top">AWS API
     *      Documentation</a>
     */
    default ListCertificatesPublisher listCertificatesPaginator(Consumer<ListCertificatesRequest.Builder> listCertificatesRequest) {
        return listCertificatesPaginator(ListCertificatesRequest.builder().applyMutation(listCertificatesRequest).build());
    }

    /**
     * <p>
     * Lists the tags that have been applied to the ACM certificate. Use the certificate's Amazon Resource Name (ARN) to
     * specify the certificate. To add a tag to an ACM certificate, use the <a>AddTagsToCertificate</a> action. To
     * delete a tag, use the <a>RemoveTagsFromCertificate</a> action.
     * </p>
     *
     * @param listTagsForCertificateRequest
     * @return A Java Future containing the result of the ListTagsForCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ListTagsForCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ListTagsForCertificate" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<ListTagsForCertificateResponse> listTagsForCertificate(
            ListTagsForCertificateRequest listTagsForCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Lists the tags that have been applied to the ACM certificate. Use the certificate's Amazon Resource Name (ARN) to
     * specify the certificate. To add a tag to an ACM certificate, use the <a>AddTagsToCertificate</a> action. To
     * delete a tag, use the <a>RemoveTagsFromCertificate</a> action.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListTagsForCertificateRequest.Builder} avoiding the
     * need to create one manually via {@link ListTagsForCertificateRequest#builder()}
     * </p>
     *
     * @param listTagsForCertificateRequest
     *        A {@link Consumer} that will call methods on {@link ListTagsForCertificateRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the ListTagsForCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ListTagsForCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ListTagsForCertificate" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<ListTagsForCertificateResponse> listTagsForCertificate(
            Consumer<ListTagsForCertificateRequest.Builder> listTagsForCertificateRequest) {
        return listTagsForCertificate(ListTagsForCertificateRequest.builder().applyMutation(listTagsForCertificateRequest)
                .build());
    }

    /**
     * <p>
     * Adds or modifies account-level configurations in ACM.
     * </p>
     * <p>
     * The supported configuration option is <code>DaysBeforeExpiry</code>. This option specifies the number of days
     * prior to certificate expiration when ACM starts generating <code>EventBridge</code> events. ACM sends one event
     * per day per certificate until the certificate expires. By default, accounts receive events starting 45 days
     * before certificate expiration.
     * </p>
     *
     * @param putAccountConfigurationRequest
     * @return A Java Future containing the result of the PutAccountConfiguration operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ValidationException The supplied input failed to satisfy constraints of an Amazon Web Services
     *         service.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>AccessDeniedException You do not have access required to perform this action.</li>
     *         <li>ConflictException You are trying to update a resource or configuration that is already being created
     *         or updated. Wait for the previous operation to finish and try again.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.PutAccountConfiguration
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/PutAccountConfiguration" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<PutAccountConfigurationResponse> putAccountConfiguration(
            PutAccountConfigurationRequest putAccountConfigurationRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Adds or modifies account-level configurations in ACM.
     * </p>
     * <p>
     * The supported configuration option is <code>DaysBeforeExpiry</code>. This option specifies the number of days
     * prior to certificate expiration when ACM starts generating <code>EventBridge</code> events. ACM sends one event
     * per day per certificate until the certificate expires. By default, accounts receive events starting 45 days
     * before certificate expiration.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PutAccountConfigurationRequest.Builder} avoiding
     * the need to create one manually via {@link PutAccountConfigurationRequest#builder()}
     * </p>
     *
     * @param putAccountConfigurationRequest
     *        A {@link Consumer} that will call methods on {@link PutAccountConfigurationRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the PutAccountConfiguration operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ValidationException The supplied input failed to satisfy constraints of an Amazon Web Services
     *         service.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>AccessDeniedException You do not have access required to perform this action.</li>
     *         <li>ConflictException You are trying to update a resource or configuration that is already being created
     *         or updated. Wait for the previous operation to finish and try again.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.PutAccountConfiguration
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/PutAccountConfiguration" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<PutAccountConfigurationResponse> putAccountConfiguration(
            Consumer<PutAccountConfigurationRequest.Builder> putAccountConfigurationRequest) {
        return putAccountConfiguration(PutAccountConfigurationRequest.builder().applyMutation(putAccountConfigurationRequest)
                .build());
    }

    /**
     * <p>
     * Remove one or more tags from an ACM certificate. A tag consists of a key-value pair. If you do not specify the
     * value portion of the tag when calling this function, the tag will be removed regardless of value. If you specify
     * a value, the tag is removed only if it is associated with the specified value.
     * </p>
     * <p>
     * To add tags to a certificate, use the <a>AddTagsToCertificate</a> action. To view all of the tags that have been
     * applied to a specific ACM certificate, use the <a>ListTagsForCertificate</a> action.
     * </p>
     *
     * @param removeTagsFromCertificateRequest
     * @return A Java Future containing the result of the RemoveTagsFromCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>InvalidTagException One or both of the values that make up the key-value pair is not valid. For
     *         example, you cannot specify a tag value that begins with <code>aws:</code>.</li>
     *         <li>TagPolicyException A specified tag did not comply with an existing tag policy and was rejected.</li>
     *         <li>InvalidParameterException An input parameter was invalid.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.RemoveTagsFromCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/RemoveTagsFromCertificate" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<RemoveTagsFromCertificateResponse> removeTagsFromCertificate(
            RemoveTagsFromCertificateRequest removeTagsFromCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Remove one or more tags from an ACM certificate. A tag consists of a key-value pair. If you do not specify the
     * value portion of the tag when calling this function, the tag will be removed regardless of value. If you specify
     * a value, the tag is removed only if it is associated with the specified value.
     * </p>
     * <p>
     * To add tags to a certificate, use the <a>AddTagsToCertificate</a> action. To view all of the tags that have been
     * applied to a specific ACM certificate, use the <a>ListTagsForCertificate</a> action.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link RemoveTagsFromCertificateRequest.Builder} avoiding
     * the need to create one manually via {@link RemoveTagsFromCertificateRequest#builder()}
     * </p>
     *
     * @param removeTagsFromCertificateRequest
     *        A {@link Consumer} that will call methods on {@link RemoveTagsFromCertificateRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the RemoveTagsFromCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>InvalidTagException One or both of the values that make up the key-value pair is not valid. For
     *         example, you cannot specify a tag value that begins with <code>aws:</code>.</li>
     *         <li>TagPolicyException A specified tag did not comply with an existing tag policy and was rejected.</li>
     *         <li>InvalidParameterException An input parameter was invalid.</li>
     *         <li>ThrottlingException The request was denied because it exceeded a quota.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.RemoveTagsFromCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/RemoveTagsFromCertificate" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<RemoveTagsFromCertificateResponse> removeTagsFromCertificate(
            Consumer<RemoveTagsFromCertificateRequest.Builder> removeTagsFromCertificateRequest) {
        return removeTagsFromCertificate(RemoveTagsFromCertificateRequest.builder()
                .applyMutation(removeTagsFromCertificateRequest).build());
    }

    /**
     * <p>
     * Renews an eligible ACM certificate. At this time, only exported private certificates can be renewed with this
     * operation. In order to renew your Amazon Web Services Private CA certificates with ACM, you must first <a
     * href="https://docs.aws.amazon.com/privateca/latest/userguide/PcaPermissions.html">grant the ACM service principal
     * permission to do so</a>. For more information, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/manual-renewal.html">Testing Managed Renewal</a> in the
     * ACM User Guide.
     * </p>
     *
     * @param renewCertificateRequest
     * @return A Java Future containing the result of the RenewCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.RenewCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/RenewCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<RenewCertificateResponse> renewCertificate(RenewCertificateRequest renewCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Renews an eligible ACM certificate. At this time, only exported private certificates can be renewed with this
     * operation. In order to renew your Amazon Web Services Private CA certificates with ACM, you must first <a
     * href="https://docs.aws.amazon.com/privateca/latest/userguide/PcaPermissions.html">grant the ACM service principal
     * permission to do so</a>. For more information, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/manual-renewal.html">Testing Managed Renewal</a> in the
     * ACM User Guide.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link RenewCertificateRequest.Builder} avoiding the need
     * to create one manually via {@link RenewCertificateRequest#builder()}
     * </p>
     *
     * @param renewCertificateRequest
     *        A {@link Consumer} that will call methods on {@link RenewCertificateRequest.Builder} to create a request.
     * @return A Java Future containing the result of the RenewCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.RenewCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/RenewCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<RenewCertificateResponse> renewCertificate(
            Consumer<RenewCertificateRequest.Builder> renewCertificateRequest) {
        return renewCertificate(RenewCertificateRequest.builder().applyMutation(renewCertificateRequest).build());
    }

    /**
     * <p>
     * Requests an ACM certificate for use with other Amazon Web Services services. To request an ACM certificate, you
     * must specify a fully qualified domain name (FQDN) in the <code>DomainName</code> parameter. You can also specify
     * additional FQDNs in the <code>SubjectAlternativeNames</code> parameter.
     * </p>
     * <p>
     * If you are requesting a private certificate, domain validation is not required. If you are requesting a public
     * certificate, each domain name that you specify must be validated to verify that you own or control the domain.
     * You can use <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">DNS
     * validation</a> or <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">email
     * validation</a>. We recommend that you use DNS validation. ACM issues public certificates after receiving approval
     * from the domain owner.
     * </p>
     * <note>
     * <p>
     * ACM behavior differs from the <a href="https://datatracker.ietf.org/doc/html/rfc6125#appendix-B.2">RFC 6125</a>
     * specification of the certificate validation process. ACM first checks for a Subject Alternative Name, and, if it
     * finds one, ignores the common name (CN).
     * </p>
     * </note>
     * <p>
     * After successful completion of the <code>RequestCertificate</code> action, there is a delay of several seconds
     * before you can retrieve information about the new certificate.
     * </p>
     *
     * @param requestCertificateRequest
     * @return A Java Future containing the result of the RequestCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException An ACM quota has been exceeded.</li>
     *         <li>InvalidDomainValidationOptionsException One or more values in the <a>DomainValidationOption</a>
     *         structure is incorrect.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>InvalidTagException One or both of the values that make up the key-value pair is not valid. For
     *         example, you cannot specify a tag value that begins with <code>aws:</code>.</li>
     *         <li>TooManyTagsException The request contains too many tags. Try the request again with fewer tags.</li>
     *         <li>TagPolicyException A specified tag did not comply with an existing tag policy and was rejected.</li>
     *         <li>InvalidParameterException An input parameter was invalid.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.RequestCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/RequestCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<RequestCertificateResponse> requestCertificate(RequestCertificateRequest requestCertificateRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Requests an ACM certificate for use with other Amazon Web Services services. To request an ACM certificate, you
     * must specify a fully qualified domain name (FQDN) in the <code>DomainName</code> parameter. You can also specify
     * additional FQDNs in the <code>SubjectAlternativeNames</code> parameter.
     * </p>
     * <p>
     * If you are requesting a private certificate, domain validation is not required. If you are requesting a public
     * certificate, each domain name that you specify must be validated to verify that you own or control the domain.
     * You can use <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">DNS
     * validation</a> or <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">email
     * validation</a>. We recommend that you use DNS validation. ACM issues public certificates after receiving approval
     * from the domain owner.
     * </p>
     * <note>
     * <p>
     * ACM behavior differs from the <a href="https://datatracker.ietf.org/doc/html/rfc6125#appendix-B.2">RFC 6125</a>
     * specification of the certificate validation process. ACM first checks for a Subject Alternative Name, and, if it
     * finds one, ignores the common name (CN).
     * </p>
     * </note>
     * <p>
     * After successful completion of the <code>RequestCertificate</code> action, there is a delay of several seconds
     * before you can retrieve information about the new certificate.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link RequestCertificateRequest.Builder} avoiding the
     * need to create one manually via {@link RequestCertificateRequest#builder()}
     * </p>
     *
     * @param requestCertificateRequest
     *        A {@link Consumer} that will call methods on {@link RequestCertificateRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the RequestCertificate operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>LimitExceededException An ACM quota has been exceeded.</li>
     *         <li>InvalidDomainValidationOptionsException One or more values in the <a>DomainValidationOption</a>
     *         structure is incorrect.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>InvalidTagException One or both of the values that make up the key-value pair is not valid. For
     *         example, you cannot specify a tag value that begins with <code>aws:</code>.</li>
     *         <li>TooManyTagsException The request contains too many tags. Try the request again with fewer tags.</li>
     *         <li>TagPolicyException A specified tag did not comply with an existing tag policy and was rejected.</li>
     *         <li>InvalidParameterException An input parameter was invalid.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.RequestCertificate
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/RequestCertificate" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<RequestCertificateResponse> requestCertificate(
            Consumer<RequestCertificateRequest.Builder> requestCertificateRequest) {
        return requestCertificate(RequestCertificateRequest.builder().applyMutation(requestCertificateRequest).build());
    }

    /**
     * <p>
     * Resends the email that requests domain ownership validation. The domain owner or an authorized representative
     * must approve the ACM certificate before it can be issued. The certificate can be approved by clicking a link in
     * the mail to navigate to the Amazon certificate approval website and then clicking <b>I Approve</b>. However, the
     * validation email can be blocked by spam filters. Therefore, if you do not receive the original mail, you can
     * request that the mail be resent within 72 hours of requesting the ACM certificate. If more than 72 hours have
     * elapsed since your original request or since your last attempt to resend validation mail, you must request a new
     * certificate. For more information about setting up your contact email addresses, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/setup-email.html">Configure Email for your Domain</a>.
     * </p>
     *
     * @param resendValidationEmailRequest
     * @return A Java Future containing the result of the ResendValidationEmail operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidStateException Processing has reached an invalid state.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>InvalidDomainValidationOptionsException One or more values in the <a>DomainValidationOption</a>
     *         structure is incorrect.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ResendValidationEmail
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ResendValidationEmail" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ResendValidationEmailResponse> resendValidationEmail(
            ResendValidationEmailRequest resendValidationEmailRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Resends the email that requests domain ownership validation. The domain owner or an authorized representative
     * must approve the ACM certificate before it can be issued. The certificate can be approved by clicking a link in
     * the mail to navigate to the Amazon certificate approval website and then clicking <b>I Approve</b>. However, the
     * validation email can be blocked by spam filters. Therefore, if you do not receive the original mail, you can
     * request that the mail be resent within 72 hours of requesting the ACM certificate. If more than 72 hours have
     * elapsed since your original request or since your last attempt to resend validation mail, you must request a new
     * certificate. For more information about setting up your contact email addresses, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/setup-email.html">Configure Email for your Domain</a>.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ResendValidationEmailRequest.Builder} avoiding the
     * need to create one manually via {@link ResendValidationEmailRequest#builder()}
     * </p>
     *
     * @param resendValidationEmailRequest
     *        A {@link Consumer} that will call methods on {@link ResendValidationEmailRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the ResendValidationEmail operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>InvalidStateException Processing has reached an invalid state.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>InvalidDomainValidationOptionsException One or more values in the <a>DomainValidationOption</a>
     *         structure is incorrect.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.ResendValidationEmail
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/ResendValidationEmail" target="_top">AWS API
     *      Documentation</a>
     */
    default CompletableFuture<ResendValidationEmailResponse> resendValidationEmail(
            Consumer<ResendValidationEmailRequest.Builder> resendValidationEmailRequest) {
        return resendValidationEmail(ResendValidationEmailRequest.builder().applyMutation(resendValidationEmailRequest).build());
    }

    /**
     * <p>
     * Updates a certificate. Currently, you can use this function to specify whether to opt in to or out of recording
     * your certificate in a certificate transparency log. For more information, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-bestpractices.html#best-practices-transparency">
     * Opting Out of Certificate Transparency Logging</a>.
     * </p>
     *
     * @param updateCertificateOptionsRequest
     * @return A Java Future containing the result of the UpdateCertificateOptions operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>LimitExceededException An ACM quota has been exceeded.</li>
     *         <li>InvalidStateException Processing has reached an invalid state.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.UpdateCertificateOptions
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/UpdateCertificateOptions" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<UpdateCertificateOptionsResponse> updateCertificateOptions(
            UpdateCertificateOptionsRequest updateCertificateOptionsRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Updates a certificate. Currently, you can use this function to specify whether to opt in to or out of recording
     * your certificate in a certificate transparency log. For more information, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-bestpractices.html#best-practices-transparency">
     * Opting Out of Certificate Transparency Logging</a>.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link UpdateCertificateOptionsRequest.Builder} avoiding
     * the need to create one manually via {@link UpdateCertificateOptionsRequest#builder()}
     * </p>
     *
     * @param updateCertificateOptionsRequest
     *        A {@link Consumer} that will call methods on {@link UpdateCertificateOptionsRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the UpdateCertificateOptions operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ResourceNotFoundException The specified certificate cannot be found in the caller's account or the
     *         caller's account cannot be found.</li>
     *         <li>LimitExceededException An ACM quota has been exceeded.</li>
     *         <li>InvalidStateException Processing has reached an invalid state.</li>
     *         <li>InvalidArnException The requested Amazon Resource Name (ARN) does not refer to an existing resource.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>AcmException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample AcmAsyncClient.UpdateCertificateOptions
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/acm-2015-12-08/UpdateCertificateOptions" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<UpdateCertificateOptionsResponse> updateCertificateOptions(
            Consumer<UpdateCertificateOptionsRequest.Builder> updateCertificateOptionsRequest) {
        return updateCertificateOptions(UpdateCertificateOptionsRequest.builder().applyMutation(updateCertificateOptionsRequest)
                .build());
    }

    /**
     * Create an instance of {@link AcmAsyncWaiter} using this client.
     * <p>
     * Waiters created via this method are managed by the SDK and resources will be released when the service client is
     * closed.
     *
     * @return an instance of {@link AcmAsyncWaiter}
     */
    default AcmAsyncWaiter waiter() {
        throw new UnsupportedOperationException();
    }

    @Override
    default AcmServiceClientConfiguration serviceClientConfiguration() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a {@link AcmAsyncClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static AcmAsyncClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link AcmAsyncClient}.
     */
    static AcmAsyncClientBuilder builder() {
        return new DefaultAcmAsyncClientBuilder();
    }
}

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

package software.amazon.awssdk.services.codecatalyst;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.codecatalyst.model.AccessDeniedException;
import software.amazon.awssdk.services.codecatalyst.model.CodeCatalystException;
import software.amazon.awssdk.services.codecatalyst.model.CodeCatalystRequest;
import software.amazon.awssdk.services.codecatalyst.model.ConflictException;
import software.amazon.awssdk.services.codecatalyst.model.CreateAccessTokenRequest;
import software.amazon.awssdk.services.codecatalyst.model.CreateAccessTokenResponse;
import software.amazon.awssdk.services.codecatalyst.model.CreateDevEnvironmentRequest;
import software.amazon.awssdk.services.codecatalyst.model.CreateDevEnvironmentResponse;
import software.amazon.awssdk.services.codecatalyst.model.CreateProjectRequest;
import software.amazon.awssdk.services.codecatalyst.model.CreateProjectResponse;
import software.amazon.awssdk.services.codecatalyst.model.CreateSourceRepositoryBranchRequest;
import software.amazon.awssdk.services.codecatalyst.model.CreateSourceRepositoryBranchResponse;
import software.amazon.awssdk.services.codecatalyst.model.DeleteAccessTokenRequest;
import software.amazon.awssdk.services.codecatalyst.model.DeleteAccessTokenResponse;
import software.amazon.awssdk.services.codecatalyst.model.DeleteDevEnvironmentRequest;
import software.amazon.awssdk.services.codecatalyst.model.DeleteDevEnvironmentResponse;
import software.amazon.awssdk.services.codecatalyst.model.GetDevEnvironmentRequest;
import software.amazon.awssdk.services.codecatalyst.model.GetDevEnvironmentResponse;
import software.amazon.awssdk.services.codecatalyst.model.GetProjectRequest;
import software.amazon.awssdk.services.codecatalyst.model.GetProjectResponse;
import software.amazon.awssdk.services.codecatalyst.model.GetSourceRepositoryCloneUrlsRequest;
import software.amazon.awssdk.services.codecatalyst.model.GetSourceRepositoryCloneUrlsResponse;
import software.amazon.awssdk.services.codecatalyst.model.GetSpaceRequest;
import software.amazon.awssdk.services.codecatalyst.model.GetSpaceResponse;
import software.amazon.awssdk.services.codecatalyst.model.GetSubscriptionRequest;
import software.amazon.awssdk.services.codecatalyst.model.GetSubscriptionResponse;
import software.amazon.awssdk.services.codecatalyst.model.GetUserDetailsRequest;
import software.amazon.awssdk.services.codecatalyst.model.GetUserDetailsResponse;
import software.amazon.awssdk.services.codecatalyst.model.ListAccessTokensRequest;
import software.amazon.awssdk.services.codecatalyst.model.ListAccessTokensResponse;
import software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentSessionsRequest;
import software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentSessionsResponse;
import software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentsRequest;
import software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentsResponse;
import software.amazon.awssdk.services.codecatalyst.model.ListEventLogsRequest;
import software.amazon.awssdk.services.codecatalyst.model.ListEventLogsResponse;
import software.amazon.awssdk.services.codecatalyst.model.ListProjectsRequest;
import software.amazon.awssdk.services.codecatalyst.model.ListProjectsResponse;
import software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoriesRequest;
import software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoriesResponse;
import software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoryBranchesRequest;
import software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoryBranchesResponse;
import software.amazon.awssdk.services.codecatalyst.model.ListSpacesRequest;
import software.amazon.awssdk.services.codecatalyst.model.ListSpacesResponse;
import software.amazon.awssdk.services.codecatalyst.model.ResourceNotFoundException;
import software.amazon.awssdk.services.codecatalyst.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.codecatalyst.model.StartDevEnvironmentRequest;
import software.amazon.awssdk.services.codecatalyst.model.StartDevEnvironmentResponse;
import software.amazon.awssdk.services.codecatalyst.model.StartDevEnvironmentSessionRequest;
import software.amazon.awssdk.services.codecatalyst.model.StartDevEnvironmentSessionResponse;
import software.amazon.awssdk.services.codecatalyst.model.StopDevEnvironmentRequest;
import software.amazon.awssdk.services.codecatalyst.model.StopDevEnvironmentResponse;
import software.amazon.awssdk.services.codecatalyst.model.StopDevEnvironmentSessionRequest;
import software.amazon.awssdk.services.codecatalyst.model.StopDevEnvironmentSessionResponse;
import software.amazon.awssdk.services.codecatalyst.model.ThrottlingException;
import software.amazon.awssdk.services.codecatalyst.model.UpdateDevEnvironmentRequest;
import software.amazon.awssdk.services.codecatalyst.model.UpdateDevEnvironmentResponse;
import software.amazon.awssdk.services.codecatalyst.model.ValidationException;
import software.amazon.awssdk.services.codecatalyst.model.VerifySessionRequest;
import software.amazon.awssdk.services.codecatalyst.model.VerifySessionResponse;
import software.amazon.awssdk.services.codecatalyst.paginators.ListAccessTokensPublisher;
import software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentSessionsPublisher;
import software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentsPublisher;
import software.amazon.awssdk.services.codecatalyst.paginators.ListEventLogsPublisher;
import software.amazon.awssdk.services.codecatalyst.paginators.ListProjectsPublisher;
import software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoriesPublisher;
import software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoryBranchesPublisher;
import software.amazon.awssdk.services.codecatalyst.paginators.ListSpacesPublisher;
import software.amazon.awssdk.services.codecatalyst.transform.CreateAccessTokenRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.CreateDevEnvironmentRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.CreateProjectRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.CreateSourceRepositoryBranchRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.DeleteAccessTokenRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.DeleteDevEnvironmentRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.GetDevEnvironmentRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.GetProjectRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.GetSourceRepositoryCloneUrlsRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.GetSpaceRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.GetSubscriptionRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.GetUserDetailsRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.ListAccessTokensRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.ListDevEnvironmentSessionsRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.ListDevEnvironmentsRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.ListEventLogsRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.ListProjectsRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.ListSourceRepositoriesRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.ListSourceRepositoryBranchesRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.ListSpacesRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.StartDevEnvironmentRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.StartDevEnvironmentSessionRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.StopDevEnvironmentRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.StopDevEnvironmentSessionRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.UpdateDevEnvironmentRequestMarshaller;
import software.amazon.awssdk.services.codecatalyst.transform.VerifySessionRequestMarshaller;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Internal implementation of {@link CodeCatalystAsyncClient}.
 *
 * @see CodeCatalystAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultCodeCatalystAsyncClient implements CodeCatalystAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultCodeCatalystAsyncClient.class);

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final CodeCatalystServiceClientConfiguration serviceClientConfiguration;

    protected DefaultCodeCatalystAsyncClient(CodeCatalystServiceClientConfiguration serviceClientConfiguration,
            SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.serviceClientConfiguration = serviceClientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
    }

    /**
     * <p>
     * Creates a personal access token (PAT) for the current user. A personal access token (PAT) is similar to a
     * password. It is associated with your user identity for use across all spaces and projects in Amazon CodeCatalyst.
     * You use PATs to access CodeCatalyst from resources that include integrated development environments (IDEs) and
     * Git-based source repositories. PATs represent you in Amazon CodeCatalyst and you can manage them in your user
     * settings.For more information, see <a
     * href="https://docs.aws.amazon.com/codecatalyst/latest/userguide/ipa-tokens-keys.html">Managing personal access
     * tokens in Amazon CodeCatalyst</a>.
     * </p>
     *
     * @param createAccessTokenRequest
     * @return A Java Future containing the result of the CreateAccessToken operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.CreateAccessToken
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/CreateAccessToken"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<CreateAccessTokenResponse> createAccessToken(CreateAccessTokenRequest createAccessTokenRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, createAccessTokenRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "CreateAccessToken");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<CreateAccessTokenResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, CreateAccessTokenResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<CreateAccessTokenResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<CreateAccessTokenRequest, CreateAccessTokenResponse>()
                            .withOperationName("CreateAccessToken")
                            .withMarshaller(new CreateAccessTokenRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(createAccessTokenRequest));
            CompletableFuture<CreateAccessTokenResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Creates a Dev Environment in Amazon CodeCatalyst, a cloud-based development environment that you can use to
     * quickly work on the code stored in the source repositories of your project.
     * </p>
     * <note>
     * <p>
     * When created in the Amazon CodeCatalyst console, by default a Dev Environment is configured to have a 2 core
     * processor, 4GB of RAM, and 16GB of persistent storage. None of these defaults apply to a Dev Environment created
     * programmatically.
     * </p>
     * </note>
     *
     * @param createDevEnvironmentRequest
     * @return A Java Future containing the result of the CreateDevEnvironment operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.CreateDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/CreateDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<CreateDevEnvironmentResponse> createDevEnvironment(
            CreateDevEnvironmentRequest createDevEnvironmentRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, createDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "CreateDevEnvironment");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<CreateDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, CreateDevEnvironmentResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<CreateDevEnvironmentResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<CreateDevEnvironmentRequest, CreateDevEnvironmentResponse>()
                            .withOperationName("CreateDevEnvironment")
                            .withMarshaller(new CreateDevEnvironmentRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(createDevEnvironmentRequest));
            CompletableFuture<CreateDevEnvironmentResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Creates a project in a specified space.
     * </p>
     *
     * @param createProjectRequest
     * @return A Java Future containing the result of the CreateProject operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.CreateProject
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/CreateProject" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<CreateProjectResponse> createProject(CreateProjectRequest createProjectRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, createProjectRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "CreateProject");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<CreateProjectResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    CreateProjectResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<CreateProjectResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<CreateProjectRequest, CreateProjectResponse>()
                            .withOperationName("CreateProject")
                            .withMarshaller(new CreateProjectRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(createProjectRequest));
            CompletableFuture<CreateProjectResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Creates a branch in a specified source repository in Amazon CodeCatalyst.
     * </p>
     * <note>
     * <p>
     * This API only creates a branch in a source repository hosted in Amazon CodeCatalyst. You cannot use this API to
     * create a branch in a linked repository.
     * </p>
     * </note>
     *
     * @param createSourceRepositoryBranchRequest
     * @return A Java Future containing the result of the CreateSourceRepositoryBranch operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.CreateSourceRepositoryBranch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/CreateSourceRepositoryBranch"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<CreateSourceRepositoryBranchResponse> createSourceRepositoryBranch(
            CreateSourceRepositoryBranchRequest createSourceRepositoryBranchRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, createSourceRepositoryBranchRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "CreateSourceRepositoryBranch");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<CreateSourceRepositoryBranchResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, CreateSourceRepositoryBranchResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<CreateSourceRepositoryBranchResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<CreateSourceRepositoryBranchRequest, CreateSourceRepositoryBranchResponse>()
                            .withOperationName("CreateSourceRepositoryBranch")
                            .withMarshaller(new CreateSourceRepositoryBranchRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(createSourceRepositoryBranchRequest));
            CompletableFuture<CreateSourceRepositoryBranchResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Deletes a specified personal access token (PAT). A personal access token can only be deleted by the user who
     * created it.
     * </p>
     *
     * @param deleteAccessTokenRequest
     * @return A Java Future containing the result of the DeleteAccessToken operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.DeleteAccessToken
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/DeleteAccessToken"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<DeleteAccessTokenResponse> deleteAccessToken(DeleteAccessTokenRequest deleteAccessTokenRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteAccessTokenRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteAccessToken");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<DeleteAccessTokenResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, DeleteAccessTokenResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<DeleteAccessTokenResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<DeleteAccessTokenRequest, DeleteAccessTokenResponse>()
                            .withOperationName("DeleteAccessToken")
                            .withMarshaller(new DeleteAccessTokenRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(deleteAccessTokenRequest));
            CompletableFuture<DeleteAccessTokenResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Deletes a Dev Environment.
     * </p>
     *
     * @param deleteDevEnvironmentRequest
     * @return A Java Future containing the result of the DeleteDevEnvironment operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.DeleteDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/DeleteDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<DeleteDevEnvironmentResponse> deleteDevEnvironment(
            DeleteDevEnvironmentRequest deleteDevEnvironmentRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteDevEnvironment");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<DeleteDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, DeleteDevEnvironmentResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<DeleteDevEnvironmentResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<DeleteDevEnvironmentRequest, DeleteDevEnvironmentResponse>()
                            .withOperationName("DeleteDevEnvironment")
                            .withMarshaller(new DeleteDevEnvironmentRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(deleteDevEnvironmentRequest));
            CompletableFuture<DeleteDevEnvironmentResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Returns information about a Dev Environment for a source repository in a project. Dev Environments are specific
     * to the user who creates them.
     * </p>
     *
     * @param getDevEnvironmentRequest
     * @return A Java Future containing the result of the GetDevEnvironment operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.GetDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<GetDevEnvironmentResponse> getDevEnvironment(GetDevEnvironmentRequest getDevEnvironmentRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetDevEnvironment");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<GetDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, GetDevEnvironmentResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<GetDevEnvironmentResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<GetDevEnvironmentRequest, GetDevEnvironmentResponse>()
                            .withOperationName("GetDevEnvironment")
                            .withMarshaller(new GetDevEnvironmentRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(getDevEnvironmentRequest));
            CompletableFuture<GetDevEnvironmentResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Returns information about a project.
     * </p>
     *
     * @param getProjectRequest
     * @return A Java Future containing the result of the GetProject operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.GetProject
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetProject" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<GetProjectResponse> getProject(GetProjectRequest getProjectRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getProjectRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetProject");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<GetProjectResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    GetProjectResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<GetProjectResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<GetProjectRequest, GetProjectResponse>().withOperationName("GetProject")
                            .withMarshaller(new GetProjectRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(getProjectRequest));
            CompletableFuture<GetProjectResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Returns information about the URLs that can be used with a Git client to clone a source repository.
     * </p>
     *
     * @param getSourceRepositoryCloneUrlsRequest
     * @return A Java Future containing the result of the GetSourceRepositoryCloneUrls operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.GetSourceRepositoryCloneUrls
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetSourceRepositoryCloneUrls"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<GetSourceRepositoryCloneUrlsResponse> getSourceRepositoryCloneUrls(
            GetSourceRepositoryCloneUrlsRequest getSourceRepositoryCloneUrlsRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getSourceRepositoryCloneUrlsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetSourceRepositoryCloneUrls");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<GetSourceRepositoryCloneUrlsResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, GetSourceRepositoryCloneUrlsResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<GetSourceRepositoryCloneUrlsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<GetSourceRepositoryCloneUrlsRequest, GetSourceRepositoryCloneUrlsResponse>()
                            .withOperationName("GetSourceRepositoryCloneUrls")
                            .withMarshaller(new GetSourceRepositoryCloneUrlsRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(getSourceRepositoryCloneUrlsRequest));
            CompletableFuture<GetSourceRepositoryCloneUrlsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Returns information about an space.
     * </p>
     *
     * @param getSpaceRequest
     * @return A Java Future containing the result of the GetSpace operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.GetSpace
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetSpace" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<GetSpaceResponse> getSpace(GetSpaceRequest getSpaceRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getSpaceRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetSpace");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<GetSpaceResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    GetSpaceResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<GetSpaceResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<GetSpaceRequest, GetSpaceResponse>().withOperationName("GetSpace")
                            .withMarshaller(new GetSpaceRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).withMetricCollector(apiCallMetricCollector)
                            .credentialType(CredentialType.TOKEN).withInput(getSpaceRequest));
            CompletableFuture<GetSpaceResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Returns information about the Amazon Web Services account used for billing purposes and the billing plan for the
     * space.
     * </p>
     *
     * @param getSubscriptionRequest
     * @return A Java Future containing the result of the GetSubscription operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.GetSubscription
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetSubscription" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<GetSubscriptionResponse> getSubscription(GetSubscriptionRequest getSubscriptionRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getSubscriptionRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetSubscription");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<GetSubscriptionResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, GetSubscriptionResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<GetSubscriptionResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<GetSubscriptionRequest, GetSubscriptionResponse>()
                            .withOperationName("GetSubscription")
                            .withMarshaller(new GetSubscriptionRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(getSubscriptionRequest));
            CompletableFuture<GetSubscriptionResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Returns information about a user.
     * </p>
     *
     * @param getUserDetailsRequest
     * @return A Java Future containing the result of the GetUserDetails operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.GetUserDetails
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetUserDetails" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<GetUserDetailsResponse> getUserDetails(GetUserDetailsRequest getUserDetailsRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getUserDetailsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetUserDetails");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<GetUserDetailsResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, GetUserDetailsResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<GetUserDetailsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<GetUserDetailsRequest, GetUserDetailsResponse>()
                            .withOperationName("GetUserDetails")
                            .withMarshaller(new GetUserDetailsRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(getUserDetailsRequest));
            CompletableFuture<GetUserDetailsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Lists all personal access tokens (PATs) associated with the user who calls the API. You can only list PATs
     * associated with your Amazon Web Services Builder ID.
     * </p>
     *
     * @param listAccessTokensRequest
     * @return A Java Future containing the result of the ListAccessTokens operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListAccessTokens
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListAccessTokens" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<ListAccessTokensResponse> listAccessTokens(ListAccessTokensRequest listAccessTokensRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listAccessTokensRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListAccessTokens");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<ListAccessTokensResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, ListAccessTokensResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ListAccessTokensResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<ListAccessTokensRequest, ListAccessTokensResponse>()
                            .withOperationName("ListAccessTokens")
                            .withMarshaller(new ListAccessTokensRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(listAccessTokensRequest));
            CompletableFuture<ListAccessTokensResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Lists all personal access tokens (PATs) associated with the user who calls the API. You can only list PATs
     * associated with your Amazon Web Services Builder ID.
     * </p>
     * <br/>
     * <p>
     * This is a variant of
     * {@link #listAccessTokens(software.amazon.awssdk.services.codecatalyst.model.ListAccessTokensRequest)} operation.
     * The return type is a custom publisher that can be subscribed to request a stream of response pages. SDK will
     * internally handle making service calls for you.
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
     * software.amazon.awssdk.services.codecatalyst.paginators.ListAccessTokensPublisher publisher = client.listAccessTokensPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListAccessTokensPublisher publisher = client.listAccessTokensPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.codecatalyst.model.ListAccessTokensResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.codecatalyst.model.ListAccessTokensResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of maxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listAccessTokens(software.amazon.awssdk.services.codecatalyst.model.ListAccessTokensRequest)}
     * operation.</b>
     * </p>
     *
     * @param listAccessTokensRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListAccessTokens
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListAccessTokens" target="_top">AWS
     *      API Documentation</a>
     */
    public ListAccessTokensPublisher listAccessTokensPaginator(ListAccessTokensRequest listAccessTokensRequest) {
        return new ListAccessTokensPublisher(this, applyPaginatorUserAgent(listAccessTokensRequest));
    }

    /**
     * <p>
     * Retrieves a list of active sessions for a Dev Environment in a project.
     * </p>
     *
     * @param listDevEnvironmentSessionsRequest
     * @return A Java Future containing the result of the ListDevEnvironmentSessions operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListDevEnvironmentSessions
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListDevEnvironmentSessions"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<ListDevEnvironmentSessionsResponse> listDevEnvironmentSessions(
            ListDevEnvironmentSessionsRequest listDevEnvironmentSessionsRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listDevEnvironmentSessionsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListDevEnvironmentSessions");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<ListDevEnvironmentSessionsResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, ListDevEnvironmentSessionsResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ListDevEnvironmentSessionsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<ListDevEnvironmentSessionsRequest, ListDevEnvironmentSessionsResponse>()
                            .withOperationName("ListDevEnvironmentSessions")
                            .withMarshaller(new ListDevEnvironmentSessionsRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(listDevEnvironmentSessionsRequest));
            CompletableFuture<ListDevEnvironmentSessionsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Retrieves a list of active sessions for a Dev Environment in a project.
     * </p>
     * <br/>
     * <p>
     * This is a variant of
     * {@link #listDevEnvironmentSessions(software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentSessionsRequest)}
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
     * software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentSessionsPublisher publisher = client.listDevEnvironmentSessionsPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentSessionsPublisher publisher = client.listDevEnvironmentSessionsPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentSessionsResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentSessionsResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of maxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listDevEnvironmentSessions(software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentSessionsRequest)}
     * operation.</b>
     * </p>
     *
     * @param listDevEnvironmentSessionsRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListDevEnvironmentSessions
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListDevEnvironmentSessions"
     *      target="_top">AWS API Documentation</a>
     */
    public ListDevEnvironmentSessionsPublisher listDevEnvironmentSessionsPaginator(
            ListDevEnvironmentSessionsRequest listDevEnvironmentSessionsRequest) {
        return new ListDevEnvironmentSessionsPublisher(this, applyPaginatorUserAgent(listDevEnvironmentSessionsRequest));
    }

    /**
     * <p>
     * Retrieves a list of Dev Environments in a project.
     * </p>
     *
     * @param listDevEnvironmentsRequest
     * @return A Java Future containing the result of the ListDevEnvironments operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListDevEnvironments
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListDevEnvironments"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<ListDevEnvironmentsResponse> listDevEnvironments(
            ListDevEnvironmentsRequest listDevEnvironmentsRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listDevEnvironmentsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListDevEnvironments");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<ListDevEnvironmentsResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, ListDevEnvironmentsResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ListDevEnvironmentsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<ListDevEnvironmentsRequest, ListDevEnvironmentsResponse>()
                            .withOperationName("ListDevEnvironments")
                            .withMarshaller(new ListDevEnvironmentsRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(listDevEnvironmentsRequest));
            CompletableFuture<ListDevEnvironmentsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Retrieves a list of Dev Environments in a project.
     * </p>
     * <br/>
     * <p>
     * This is a variant of
     * {@link #listDevEnvironments(software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentsRequest)}
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
     * software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentsPublisher publisher = client.listDevEnvironmentsPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentsPublisher publisher = client.listDevEnvironmentsPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentsResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentsResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of maxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listDevEnvironments(software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentsRequest)}
     * operation.</b>
     * </p>
     *
     * @param listDevEnvironmentsRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListDevEnvironments
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListDevEnvironments"
     *      target="_top">AWS API Documentation</a>
     */
    public ListDevEnvironmentsPublisher listDevEnvironmentsPaginator(ListDevEnvironmentsRequest listDevEnvironmentsRequest) {
        return new ListDevEnvironmentsPublisher(this, applyPaginatorUserAgent(listDevEnvironmentsRequest));
    }

    /**
     * <p>
     * Retrieves a list of events that occurred during a specified time period in a space. You can use these events to
     * audit user and system activity in a space.
     * </p>
     *
     * @param listEventLogsRequest
     * @return A Java Future containing the result of the ListEventLogs operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListEventLogs
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListEventLogs" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<ListEventLogsResponse> listEventLogs(ListEventLogsRequest listEventLogsRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listEventLogsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListEventLogs");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<ListEventLogsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    ListEventLogsResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ListEventLogsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<ListEventLogsRequest, ListEventLogsResponse>()
                            .withOperationName("ListEventLogs")
                            .withMarshaller(new ListEventLogsRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(listEventLogsRequest));
            CompletableFuture<ListEventLogsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Retrieves a list of events that occurred during a specified time period in a space. You can use these events to
     * audit user and system activity in a space.
     * </p>
     * <br/>
     * <p>
     * This is a variant of
     * {@link #listEventLogs(software.amazon.awssdk.services.codecatalyst.model.ListEventLogsRequest)} operation. The
     * return type is a custom publisher that can be subscribed to request a stream of response pages. SDK will
     * internally handle making service calls for you.
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
     * software.amazon.awssdk.services.codecatalyst.paginators.ListEventLogsPublisher publisher = client.listEventLogsPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListEventLogsPublisher publisher = client.listEventLogsPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.codecatalyst.model.ListEventLogsResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.codecatalyst.model.ListEventLogsResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of maxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listEventLogs(software.amazon.awssdk.services.codecatalyst.model.ListEventLogsRequest)} operation.</b>
     * </p>
     *
     * @param listEventLogsRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListEventLogs
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListEventLogs" target="_top">AWS
     *      API Documentation</a>
     */
    public ListEventLogsPublisher listEventLogsPaginator(ListEventLogsRequest listEventLogsRequest) {
        return new ListEventLogsPublisher(this, applyPaginatorUserAgent(listEventLogsRequest));
    }

    /**
     * <p>
     * Retrieves a list of projects.
     * </p>
     *
     * @param listProjectsRequest
     * @return A Java Future containing the result of the ListProjects operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListProjects
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListProjects" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<ListProjectsResponse> listProjects(ListProjectsRequest listProjectsRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listProjectsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListProjects");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<ListProjectsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    ListProjectsResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ListProjectsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<ListProjectsRequest, ListProjectsResponse>()
                            .withOperationName("ListProjects").withMarshaller(new ListProjectsRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(listProjectsRequest));
            CompletableFuture<ListProjectsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Retrieves a list of projects.
     * </p>
     * <br/>
     * <p>
     * This is a variant of
     * {@link #listProjects(software.amazon.awssdk.services.codecatalyst.model.ListProjectsRequest)} operation. The
     * return type is a custom publisher that can be subscribed to request a stream of response pages. SDK will
     * internally handle making service calls for you.
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
     * software.amazon.awssdk.services.codecatalyst.paginators.ListProjectsPublisher publisher = client.listProjectsPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListProjectsPublisher publisher = client.listProjectsPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.codecatalyst.model.ListProjectsResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.codecatalyst.model.ListProjectsResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of maxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listProjects(software.amazon.awssdk.services.codecatalyst.model.ListProjectsRequest)} operation.</b>
     * </p>
     *
     * @param listProjectsRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListProjects
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListProjects" target="_top">AWS API
     *      Documentation</a>
     */
    public ListProjectsPublisher listProjectsPaginator(ListProjectsRequest listProjectsRequest) {
        return new ListProjectsPublisher(this, applyPaginatorUserAgent(listProjectsRequest));
    }

    /**
     * <p>
     * Retrieves a list of source repositories in a project.
     * </p>
     *
     * @param listSourceRepositoriesRequest
     * @return A Java Future containing the result of the ListSourceRepositories operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListSourceRepositories
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSourceRepositories"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<ListSourceRepositoriesResponse> listSourceRepositories(
            ListSourceRepositoriesRequest listSourceRepositoriesRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listSourceRepositoriesRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListSourceRepositories");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<ListSourceRepositoriesResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, ListSourceRepositoriesResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ListSourceRepositoriesResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<ListSourceRepositoriesRequest, ListSourceRepositoriesResponse>()
                            .withOperationName("ListSourceRepositories")
                            .withMarshaller(new ListSourceRepositoriesRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(listSourceRepositoriesRequest));
            CompletableFuture<ListSourceRepositoriesResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Retrieves a list of source repositories in a project.
     * </p>
     * <br/>
     * <p>
     * This is a variant of
     * {@link #listSourceRepositories(software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoriesRequest)}
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
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoriesPublisher publisher = client.listSourceRepositoriesPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoriesPublisher publisher = client.listSourceRepositoriesPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoriesResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoriesResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of maxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listSourceRepositories(software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoriesRequest)}
     * operation.</b>
     * </p>
     *
     * @param listSourceRepositoriesRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListSourceRepositories
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSourceRepositories"
     *      target="_top">AWS API Documentation</a>
     */
    public ListSourceRepositoriesPublisher listSourceRepositoriesPaginator(
            ListSourceRepositoriesRequest listSourceRepositoriesRequest) {
        return new ListSourceRepositoriesPublisher(this, applyPaginatorUserAgent(listSourceRepositoriesRequest));
    }

    /**
     * <p>
     * Retrieves a list of branches in a specified source repository.
     * </p>
     *
     * @param listSourceRepositoryBranchesRequest
     * @return A Java Future containing the result of the ListSourceRepositoryBranches operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListSourceRepositoryBranches
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSourceRepositoryBranches"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<ListSourceRepositoryBranchesResponse> listSourceRepositoryBranches(
            ListSourceRepositoryBranchesRequest listSourceRepositoryBranchesRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listSourceRepositoryBranchesRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListSourceRepositoryBranches");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<ListSourceRepositoryBranchesResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, ListSourceRepositoryBranchesResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ListSourceRepositoryBranchesResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<ListSourceRepositoryBranchesRequest, ListSourceRepositoryBranchesResponse>()
                            .withOperationName("ListSourceRepositoryBranches")
                            .withMarshaller(new ListSourceRepositoryBranchesRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(listSourceRepositoryBranchesRequest));
            CompletableFuture<ListSourceRepositoryBranchesResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Retrieves a list of branches in a specified source repository.
     * </p>
     * <br/>
     * <p>
     * This is a variant of
     * {@link #listSourceRepositoryBranches(software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoryBranchesRequest)}
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
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoryBranchesPublisher publisher = client.listSourceRepositoryBranchesPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoryBranchesPublisher publisher = client.listSourceRepositoryBranchesPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoryBranchesResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoryBranchesResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of maxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listSourceRepositoryBranches(software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoryBranchesRequest)}
     * operation.</b>
     * </p>
     *
     * @param listSourceRepositoryBranchesRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListSourceRepositoryBranches
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSourceRepositoryBranches"
     *      target="_top">AWS API Documentation</a>
     */
    public ListSourceRepositoryBranchesPublisher listSourceRepositoryBranchesPaginator(
            ListSourceRepositoryBranchesRequest listSourceRepositoryBranchesRequest) {
        return new ListSourceRepositoryBranchesPublisher(this, applyPaginatorUserAgent(listSourceRepositoryBranchesRequest));
    }

    /**
     * <p>
     * Retrieves a list of spaces.
     * </p>
     *
     * @param listSpacesRequest
     * @return A Java Future containing the result of the ListSpaces operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListSpaces
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSpaces" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<ListSpacesResponse> listSpaces(ListSpacesRequest listSpacesRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listSpacesRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListSpaces");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<ListSpacesResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    ListSpacesResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<ListSpacesResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<ListSpacesRequest, ListSpacesResponse>().withOperationName("ListSpaces")
                            .withMarshaller(new ListSpacesRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(listSpacesRequest));
            CompletableFuture<ListSpacesResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Retrieves a list of spaces.
     * </p>
     * <br/>
     * <p>
     * This is a variant of {@link #listSpaces(software.amazon.awssdk.services.codecatalyst.model.ListSpacesRequest)}
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
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSpacesPublisher publisher = client.listSpacesPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSpacesPublisher publisher = client.listSpacesPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.codecatalyst.model.ListSpacesResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.codecatalyst.model.ListSpacesResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Please notice that the configuration of null won't limit the number of results you get with the paginator. It
     * only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #listSpaces(software.amazon.awssdk.services.codecatalyst.model.ListSpacesRequest)} operation.</b>
     * </p>
     *
     * @param listSpacesRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.ListSpaces
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSpaces" target="_top">AWS API
     *      Documentation</a>
     */
    public ListSpacesPublisher listSpacesPaginator(ListSpacesRequest listSpacesRequest) {
        return new ListSpacesPublisher(this, applyPaginatorUserAgent(listSpacesRequest));
    }

    /**
     * <p>
     * Starts a specified Dev Environment and puts it into an active state.
     * </p>
     *
     * @param startDevEnvironmentRequest
     * @return A Java Future containing the result of the StartDevEnvironment operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.StartDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/StartDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<StartDevEnvironmentResponse> startDevEnvironment(
            StartDevEnvironmentRequest startDevEnvironmentRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, startDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StartDevEnvironment");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<StartDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, StartDevEnvironmentResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<StartDevEnvironmentResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<StartDevEnvironmentRequest, StartDevEnvironmentResponse>()
                            .withOperationName("StartDevEnvironment")
                            .withMarshaller(new StartDevEnvironmentRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(startDevEnvironmentRequest));
            CompletableFuture<StartDevEnvironmentResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Starts a session for a specified Dev Environment.
     * </p>
     *
     * @param startDevEnvironmentSessionRequest
     * @return A Java Future containing the result of the StartDevEnvironmentSession operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.StartDevEnvironmentSession
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/StartDevEnvironmentSession"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<StartDevEnvironmentSessionResponse> startDevEnvironmentSession(
            StartDevEnvironmentSessionRequest startDevEnvironmentSessionRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, startDevEnvironmentSessionRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StartDevEnvironmentSession");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<StartDevEnvironmentSessionResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, StartDevEnvironmentSessionResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<StartDevEnvironmentSessionResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<StartDevEnvironmentSessionRequest, StartDevEnvironmentSessionResponse>()
                            .withOperationName("StartDevEnvironmentSession")
                            .withMarshaller(new StartDevEnvironmentSessionRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(startDevEnvironmentSessionRequest));
            CompletableFuture<StartDevEnvironmentSessionResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Pauses a specified Dev Environment and places it in a non-running state. Stopped Dev Environments do not consume
     * compute minutes.
     * </p>
     *
     * @param stopDevEnvironmentRequest
     * @return A Java Future containing the result of the StopDevEnvironment operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.StopDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/StopDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<StopDevEnvironmentResponse> stopDevEnvironment(StopDevEnvironmentRequest stopDevEnvironmentRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, stopDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StopDevEnvironment");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<StopDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, StopDevEnvironmentResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<StopDevEnvironmentResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<StopDevEnvironmentRequest, StopDevEnvironmentResponse>()
                            .withOperationName("StopDevEnvironment")
                            .withMarshaller(new StopDevEnvironmentRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(stopDevEnvironmentRequest));
            CompletableFuture<StopDevEnvironmentResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Stops a session for a specified Dev Environment.
     * </p>
     *
     * @param stopDevEnvironmentSessionRequest
     * @return A Java Future containing the result of the StopDevEnvironmentSession operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.StopDevEnvironmentSession
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/StopDevEnvironmentSession"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<StopDevEnvironmentSessionResponse> stopDevEnvironmentSession(
            StopDevEnvironmentSessionRequest stopDevEnvironmentSessionRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, stopDevEnvironmentSessionRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StopDevEnvironmentSession");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<StopDevEnvironmentSessionResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, StopDevEnvironmentSessionResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<StopDevEnvironmentSessionResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<StopDevEnvironmentSessionRequest, StopDevEnvironmentSessionResponse>()
                            .withOperationName("StopDevEnvironmentSession")
                            .withMarshaller(new StopDevEnvironmentSessionRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(stopDevEnvironmentSessionRequest));
            CompletableFuture<StopDevEnvironmentSessionResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Changes one or more values for a Dev Environment. Updating certain values of the Dev Environment will cause a
     * restart.
     * </p>
     *
     * @param updateDevEnvironmentRequest
     * @return A Java Future containing the result of the UpdateDevEnvironment operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.UpdateDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/UpdateDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<UpdateDevEnvironmentResponse> updateDevEnvironment(
            UpdateDevEnvironmentRequest updateDevEnvironmentRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, updateDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "UpdateDevEnvironment");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<UpdateDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, UpdateDevEnvironmentResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<UpdateDevEnvironmentResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<UpdateDevEnvironmentRequest, UpdateDevEnvironmentResponse>()
                            .withOperationName("UpdateDevEnvironment")
                            .withMarshaller(new UpdateDevEnvironmentRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(updateDevEnvironmentRequest));
            CompletableFuture<UpdateDevEnvironmentResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * <p>
     * Verifies whether the calling user has a valid Amazon CodeCatalyst login and session. If successful, this returns
     * the ID of the user in Amazon CodeCatalyst.
     * </p>
     *
     * @param verifySessionRequest
     * @return A Java Future containing the result of the VerifySession operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>ThrottlingException The request was denied due to request throttling.</li>
     *         <li>ConflictException The request was denied because the requested operation would cause a conflict with
     *         the current state of a service resource associated with the request. Another user might have updated the
     *         resource. Reload, make sure you have the latest data, and then try again.</li>
     *         <li>ValidationException The request was denied because an input failed to satisfy the constraints
     *         specified by the service. Check the spelling and input requirements, and then try again.</li>
     *         <li>ServiceQuotaExceededException The request was denied because one or more resources has reached its
     *         limits for the tier the space belongs to. Either reduce the number of resources, or change the tier if
     *         applicable.</li>
     *         <li>ResourceNotFoundException The request was denied because the specified resource was not found. Verify
     *         that the spelling is correct and that you have access to the resource.</li>
     *         <li>AccessDeniedException The request was denied because you don't have sufficient access to perform this
     *         action. Verify that you are a member of a role that allows this action.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>CodeCatalystException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample CodeCatalystAsyncClient.VerifySession
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/VerifySession" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<VerifySessionResponse> verifySession(VerifySessionRequest verifySessionRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, verifySessionRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "VerifySession");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<VerifySessionResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    VerifySessionResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<VerifySessionResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<VerifySessionRequest, VerifySessionResponse>()
                            .withOperationName("VerifySession")
                            .withMarshaller(new VerifySessionRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).credentialType(CredentialType.TOKEN)
                            .withInput(verifySessionRequest));
            CompletableFuture<VerifySessionResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    @Override
    public final CodeCatalystServiceClientConfiguration serviceClientConfiguration() {
        return this.serviceClientConfiguration;
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder
                .clientConfiguration(clientConfiguration)
                .defaultServiceExceptionSupplier(CodeCatalystException::builder)
                .protocol(AwsJsonProtocol.REST_JSON)
                .protocolVersion("1.1")
                .registerModeledException(
                        ExceptionMetadata.builder().errorCode("AccessDeniedException")
                                .exceptionBuilderSupplier(AccessDeniedException::builder).httpStatusCode(403).build())
                .registerModeledException(
                        ExceptionMetadata.builder().errorCode("ConflictException")
                                .exceptionBuilderSupplier(ConflictException::builder).httpStatusCode(409).build())
                .registerModeledException(
                        ExceptionMetadata.builder().errorCode("ResourceNotFoundException")
                                .exceptionBuilderSupplier(ResourceNotFoundException::builder).httpStatusCode(404).build())
                .registerModeledException(
                        ExceptionMetadata.builder().errorCode("ThrottlingException")
                                .exceptionBuilderSupplier(ThrottlingException::builder).httpStatusCode(429).build())
                .registerModeledException(
                        ExceptionMetadata.builder().errorCode("ValidationException")
                                .exceptionBuilderSupplier(ValidationException::builder).httpStatusCode(400).build())
                .registerModeledException(
                        ExceptionMetadata.builder().errorCode("ServiceQuotaExceededException")
                                .exceptionBuilderSupplier(ServiceQuotaExceededException::builder).httpStatusCode(402).build());
    }

    private static List<MetricPublisher> resolveMetricPublishers(SdkClientConfiguration clientConfiguration,
            RequestOverrideConfiguration requestOverrideConfiguration) {
        List<MetricPublisher> publishers = null;
        if (requestOverrideConfiguration != null) {
            publishers = requestOverrideConfiguration.metricPublishers();
        }
        if (publishers == null || publishers.isEmpty()) {
            publishers = clientConfiguration.option(SdkClientOption.METRIC_PUBLISHERS);
        }
        if (publishers == null) {
            publishers = Collections.emptyList();
        }
        return publishers;
    }

    private <T extends CodeCatalystRequest> T applyPaginatorUserAgent(T request) {
        Consumer<AwsRequestOverrideConfiguration.Builder> userAgentApplier = b -> b.addApiName(ApiName.builder()
                .version(VersionInfo.SDK_VERSION).name("PAGINATED").build());
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                .map(c -> c.toBuilder().applyMutation(userAgentApplier).build())
                .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
            JsonOperationMetadata operationMetadata) {
        return protocolFactory.createErrorResponseHandler(operationMetadata);
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}

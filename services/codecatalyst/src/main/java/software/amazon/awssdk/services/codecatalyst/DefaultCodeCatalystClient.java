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
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
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
import software.amazon.awssdk.services.codecatalyst.paginators.ListAccessTokensIterable;
import software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentSessionsIterable;
import software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentsIterable;
import software.amazon.awssdk.services.codecatalyst.paginators.ListEventLogsIterable;
import software.amazon.awssdk.services.codecatalyst.paginators.ListProjectsIterable;
import software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoriesIterable;
import software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoryBranchesIterable;
import software.amazon.awssdk.services.codecatalyst.paginators.ListSpacesIterable;
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
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link CodeCatalystClient}.
 *
 * @see CodeCatalystClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultCodeCatalystClient implements CodeCatalystClient {
    private static final Logger log = Logger.loggerFor(DefaultCodeCatalystClient.class);

    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final CodeCatalystServiceClientConfiguration serviceClientConfiguration;

    protected DefaultCodeCatalystClient(CodeCatalystServiceClientConfiguration serviceClientConfiguration,
            SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
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
     * @return Result of the CreateAccessToken operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.CreateAccessToken
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/CreateAccessToken"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CreateAccessTokenResponse createAccessToken(CreateAccessTokenRequest createAccessTokenRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<CreateAccessTokenResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                CreateAccessTokenResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, createAccessTokenRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "CreateAccessToken");

            return clientHandler.execute(new ClientExecutionParams<CreateAccessTokenRequest, CreateAccessTokenResponse>()
                    .withOperationName("CreateAccessToken").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(createAccessTokenRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new CreateAccessTokenRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
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
     * @return Result of the CreateDevEnvironment operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.CreateDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/CreateDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CreateDevEnvironmentResponse createDevEnvironment(CreateDevEnvironmentRequest createDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<CreateDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, CreateDevEnvironmentResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, createDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "CreateDevEnvironment");

            return clientHandler.execute(new ClientExecutionParams<CreateDevEnvironmentRequest, CreateDevEnvironmentResponse>()
                    .withOperationName("CreateDevEnvironment").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(createDevEnvironmentRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new CreateDevEnvironmentRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Creates a project in a specified space.
     * </p>
     *
     * @param createProjectRequest
     * @return Result of the CreateProject operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.CreateProject
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/CreateProject" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CreateProjectResponse createProject(CreateProjectRequest createProjectRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<CreateProjectResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                CreateProjectResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, createProjectRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "CreateProject");

            return clientHandler.execute(new ClientExecutionParams<CreateProjectRequest, CreateProjectResponse>()
                    .withOperationName("CreateProject").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(createProjectRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new CreateProjectRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
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
     * @return Result of the CreateSourceRepositoryBranch operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.CreateSourceRepositoryBranch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/CreateSourceRepositoryBranch"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CreateSourceRepositoryBranchResponse createSourceRepositoryBranch(
            CreateSourceRepositoryBranchRequest createSourceRepositoryBranchRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<CreateSourceRepositoryBranchResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, CreateSourceRepositoryBranchResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, createSourceRepositoryBranchRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "CreateSourceRepositoryBranch");

            return clientHandler
                    .execute(new ClientExecutionParams<CreateSourceRepositoryBranchRequest, CreateSourceRepositoryBranchResponse>()
                            .withOperationName("CreateSourceRepositoryBranch").withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                            .withInput(createSourceRepositoryBranchRequest).withMetricCollector(apiCallMetricCollector)
                            .withMarshaller(new CreateSourceRepositoryBranchRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Deletes a specified personal access token (PAT). A personal access token can only be deleted by the user who
     * created it.
     * </p>
     *
     * @param deleteAccessTokenRequest
     * @return Result of the DeleteAccessToken operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.DeleteAccessToken
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/DeleteAccessToken"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public DeleteAccessTokenResponse deleteAccessToken(DeleteAccessTokenRequest deleteAccessTokenRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<DeleteAccessTokenResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                DeleteAccessTokenResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteAccessTokenRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteAccessToken");

            return clientHandler.execute(new ClientExecutionParams<DeleteAccessTokenRequest, DeleteAccessTokenResponse>()
                    .withOperationName("DeleteAccessToken").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(deleteAccessTokenRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new DeleteAccessTokenRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Deletes a Dev Environment.
     * </p>
     *
     * @param deleteDevEnvironmentRequest
     * @return Result of the DeleteDevEnvironment operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.DeleteDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/DeleteDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public DeleteDevEnvironmentResponse deleteDevEnvironment(DeleteDevEnvironmentRequest deleteDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<DeleteDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, DeleteDevEnvironmentResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteDevEnvironment");

            return clientHandler.execute(new ClientExecutionParams<DeleteDevEnvironmentRequest, DeleteDevEnvironmentResponse>()
                    .withOperationName("DeleteDevEnvironment").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(deleteDevEnvironmentRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new DeleteDevEnvironmentRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Returns information about a Dev Environment for a source repository in a project. Dev Environments are specific
     * to the user who creates them.
     * </p>
     *
     * @param getDevEnvironmentRequest
     * @return Result of the GetDevEnvironment operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.GetDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public GetDevEnvironmentResponse getDevEnvironment(GetDevEnvironmentRequest getDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<GetDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                GetDevEnvironmentResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetDevEnvironment");

            return clientHandler.execute(new ClientExecutionParams<GetDevEnvironmentRequest, GetDevEnvironmentResponse>()
                    .withOperationName("GetDevEnvironment").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(getDevEnvironmentRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new GetDevEnvironmentRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Returns information about a project.
     * </p>
     *
     * @param getProjectRequest
     * @return Result of the GetProject operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.GetProject
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetProject" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public GetProjectResponse getProject(GetProjectRequest getProjectRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<GetProjectResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                GetProjectResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getProjectRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetProject");

            return clientHandler.execute(new ClientExecutionParams<GetProjectRequest, GetProjectResponse>()
                    .withOperationName("GetProject").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(getProjectRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new GetProjectRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Returns information about the URLs that can be used with a Git client to clone a source repository.
     * </p>
     *
     * @param getSourceRepositoryCloneUrlsRequest
     * @return Result of the GetSourceRepositoryCloneUrls operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.GetSourceRepositoryCloneUrls
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetSourceRepositoryCloneUrls"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public GetSourceRepositoryCloneUrlsResponse getSourceRepositoryCloneUrls(
            GetSourceRepositoryCloneUrlsRequest getSourceRepositoryCloneUrlsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<GetSourceRepositoryCloneUrlsResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, GetSourceRepositoryCloneUrlsResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getSourceRepositoryCloneUrlsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetSourceRepositoryCloneUrls");

            return clientHandler
                    .execute(new ClientExecutionParams<GetSourceRepositoryCloneUrlsRequest, GetSourceRepositoryCloneUrlsResponse>()
                            .withOperationName("GetSourceRepositoryCloneUrls").withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                            .withInput(getSourceRepositoryCloneUrlsRequest).withMetricCollector(apiCallMetricCollector)
                            .withMarshaller(new GetSourceRepositoryCloneUrlsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Returns information about an space.
     * </p>
     *
     * @param getSpaceRequest
     * @return Result of the GetSpace operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.GetSpace
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetSpace" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public GetSpaceResponse getSpace(GetSpaceRequest getSpaceRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<GetSpaceResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                GetSpaceResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getSpaceRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetSpace");

            return clientHandler.execute(new ClientExecutionParams<GetSpaceRequest, GetSpaceResponse>()
                    .withOperationName("GetSpace").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(getSpaceRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new GetSpaceRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Returns information about the Amazon Web Services account used for billing purposes and the billing plan for the
     * space.
     * </p>
     *
     * @param getSubscriptionRequest
     * @return Result of the GetSubscription operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.GetSubscription
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetSubscription" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public GetSubscriptionResponse getSubscription(GetSubscriptionRequest getSubscriptionRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<GetSubscriptionResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                GetSubscriptionResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getSubscriptionRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetSubscription");

            return clientHandler.execute(new ClientExecutionParams<GetSubscriptionRequest, GetSubscriptionResponse>()
                    .withOperationName("GetSubscription").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(getSubscriptionRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new GetSubscriptionRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Returns information about a user.
     * </p>
     *
     * @param getUserDetailsRequest
     * @return Result of the GetUserDetails operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.GetUserDetails
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/GetUserDetails" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public GetUserDetailsResponse getUserDetails(GetUserDetailsRequest getUserDetailsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<GetUserDetailsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                GetUserDetailsResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getUserDetailsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetUserDetails");

            return clientHandler.execute(new ClientExecutionParams<GetUserDetailsRequest, GetUserDetailsResponse>()
                    .withOperationName("GetUserDetails").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(getUserDetailsRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new GetUserDetailsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Lists all personal access tokens (PATs) associated with the user who calls the API. You can only list PATs
     * associated with your Amazon Web Services Builder ID.
     * </p>
     *
     * @param listAccessTokensRequest
     * @return Result of the ListAccessTokens operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListAccessTokens
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListAccessTokens" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public ListAccessTokensResponse listAccessTokens(ListAccessTokensRequest listAccessTokensRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<ListAccessTokensResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                ListAccessTokensResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listAccessTokensRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListAccessTokens");

            return clientHandler.execute(new ClientExecutionParams<ListAccessTokensRequest, ListAccessTokensResponse>()
                    .withOperationName("ListAccessTokens").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(listAccessTokensRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new ListAccessTokensRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
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
     * The return type is a custom iterable that can be used to iterate through all the pages. SDK will internally
     * handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListAccessTokensIterable responses = client.listAccessTokensPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     * 
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.codecatalyst.paginators.ListAccessTokensIterable responses = client
     *             .listAccessTokensPaginator(request);
     *     for (software.amazon.awssdk.services.codecatalyst.model.ListAccessTokensResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListAccessTokensIterable responses = client.listAccessTokensPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListAccessTokens
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListAccessTokens" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public ListAccessTokensIterable listAccessTokensPaginator(ListAccessTokensRequest listAccessTokensRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return new ListAccessTokensIterable(this, applyPaginatorUserAgent(listAccessTokensRequest));
    }

    /**
     * <p>
     * Retrieves a list of active sessions for a Dev Environment in a project.
     * </p>
     *
     * @param listDevEnvironmentSessionsRequest
     * @return Result of the ListDevEnvironmentSessions operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListDevEnvironmentSessions
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListDevEnvironmentSessions"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public ListDevEnvironmentSessionsResponse listDevEnvironmentSessions(
            ListDevEnvironmentSessionsRequest listDevEnvironmentSessionsRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<ListDevEnvironmentSessionsResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, ListDevEnvironmentSessionsResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listDevEnvironmentSessionsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListDevEnvironmentSessions");

            return clientHandler
                    .execute(new ClientExecutionParams<ListDevEnvironmentSessionsRequest, ListDevEnvironmentSessionsResponse>()
                            .withOperationName("ListDevEnvironmentSessions").withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                            .withInput(listDevEnvironmentSessionsRequest).withMetricCollector(apiCallMetricCollector)
                            .withMarshaller(new ListDevEnvironmentSessionsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
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
     * operation. The return type is a custom iterable that can be used to iterate through all the pages. SDK will
     * internally handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentSessionsIterable responses = client.listDevEnvironmentSessionsPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     * 
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentSessionsIterable responses = client
     *             .listDevEnvironmentSessionsPaginator(request);
     *     for (software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentSessionsResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentSessionsIterable responses = client.listDevEnvironmentSessionsPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListDevEnvironmentSessions
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListDevEnvironmentSessions"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public ListDevEnvironmentSessionsIterable listDevEnvironmentSessionsPaginator(
            ListDevEnvironmentSessionsRequest listDevEnvironmentSessionsRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        return new ListDevEnvironmentSessionsIterable(this, applyPaginatorUserAgent(listDevEnvironmentSessionsRequest));
    }

    /**
     * <p>
     * Retrieves a list of Dev Environments in a project.
     * </p>
     *
     * @param listDevEnvironmentsRequest
     * @return Result of the ListDevEnvironments operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListDevEnvironments
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListDevEnvironments"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public ListDevEnvironmentsResponse listDevEnvironments(ListDevEnvironmentsRequest listDevEnvironmentsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<ListDevEnvironmentsResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, ListDevEnvironmentsResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listDevEnvironmentsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListDevEnvironments");

            return clientHandler.execute(new ClientExecutionParams<ListDevEnvironmentsRequest, ListDevEnvironmentsResponse>()
                    .withOperationName("ListDevEnvironments").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(listDevEnvironmentsRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new ListDevEnvironmentsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
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
     * operation. The return type is a custom iterable that can be used to iterate through all the pages. SDK will
     * internally handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentsIterable responses = client.listDevEnvironmentsPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     * 
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentsIterable responses = client
     *             .listDevEnvironmentsPaginator(request);
     *     for (software.amazon.awssdk.services.codecatalyst.model.ListDevEnvironmentsResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListDevEnvironmentsIterable responses = client.listDevEnvironmentsPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListDevEnvironments
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListDevEnvironments"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public ListDevEnvironmentsIterable listDevEnvironmentsPaginator(ListDevEnvironmentsRequest listDevEnvironmentsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return new ListDevEnvironmentsIterable(this, applyPaginatorUserAgent(listDevEnvironmentsRequest));
    }

    /**
     * <p>
     * Retrieves a list of events that occurred during a specified time period in a space. You can use these events to
     * audit user and system activity in a space.
     * </p>
     *
     * @param listEventLogsRequest
     * @return Result of the ListEventLogs operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListEventLogs
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListEventLogs" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public ListEventLogsResponse listEventLogs(ListEventLogsRequest listEventLogsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<ListEventLogsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                ListEventLogsResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listEventLogsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListEventLogs");

            return clientHandler.execute(new ClientExecutionParams<ListEventLogsRequest, ListEventLogsResponse>()
                    .withOperationName("ListEventLogs").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(listEventLogsRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new ListEventLogsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
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
     * return type is a custom iterable that can be used to iterate through all the pages. SDK will internally handle
     * making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListEventLogsIterable responses = client.listEventLogsPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     * 
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.codecatalyst.paginators.ListEventLogsIterable responses = client
     *             .listEventLogsPaginator(request);
     *     for (software.amazon.awssdk.services.codecatalyst.model.ListEventLogsResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListEventLogsIterable responses = client.listEventLogsPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListEventLogs
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListEventLogs" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public ListEventLogsIterable listEventLogsPaginator(ListEventLogsRequest listEventLogsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return new ListEventLogsIterable(this, applyPaginatorUserAgent(listEventLogsRequest));
    }

    /**
     * <p>
     * Retrieves a list of projects.
     * </p>
     *
     * @param listProjectsRequest
     * @return Result of the ListProjects operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListProjects
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListProjects" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public ListProjectsResponse listProjects(ListProjectsRequest listProjectsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<ListProjectsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                ListProjectsResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listProjectsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListProjects");

            return clientHandler.execute(new ClientExecutionParams<ListProjectsRequest, ListProjectsResponse>()
                    .withOperationName("ListProjects").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(listProjectsRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new ListProjectsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
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
     * return type is a custom iterable that can be used to iterate through all the pages. SDK will internally handle
     * making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListProjectsIterable responses = client.listProjectsPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     * 
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.codecatalyst.paginators.ListProjectsIterable responses = client
     *             .listProjectsPaginator(request);
     *     for (software.amazon.awssdk.services.codecatalyst.model.ListProjectsResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListProjectsIterable responses = client.listProjectsPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListProjects
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListProjects" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public ListProjectsIterable listProjectsPaginator(ListProjectsRequest listProjectsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return new ListProjectsIterable(this, applyPaginatorUserAgent(listProjectsRequest));
    }

    /**
     * <p>
     * Retrieves a list of source repositories in a project.
     * </p>
     *
     * @param listSourceRepositoriesRequest
     * @return Result of the ListSourceRepositories operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListSourceRepositories
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSourceRepositories"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public ListSourceRepositoriesResponse listSourceRepositories(ListSourceRepositoriesRequest listSourceRepositoriesRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<ListSourceRepositoriesResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, ListSourceRepositoriesResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listSourceRepositoriesRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListSourceRepositories");

            return clientHandler
                    .execute(new ClientExecutionParams<ListSourceRepositoriesRequest, ListSourceRepositoriesResponse>()
                            .withOperationName("ListSourceRepositories").withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                            .withInput(listSourceRepositoriesRequest).withMetricCollector(apiCallMetricCollector)
                            .withMarshaller(new ListSourceRepositoriesRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
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
     * operation. The return type is a custom iterable that can be used to iterate through all the pages. SDK will
     * internally handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoriesIterable responses = client.listSourceRepositoriesPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     * 
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoriesIterable responses = client
     *             .listSourceRepositoriesPaginator(request);
     *     for (software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoriesResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoriesIterable responses = client.listSourceRepositoriesPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListSourceRepositories
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSourceRepositories"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public ListSourceRepositoriesIterable listSourceRepositoriesPaginator(
            ListSourceRepositoriesRequest listSourceRepositoriesRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        return new ListSourceRepositoriesIterable(this, applyPaginatorUserAgent(listSourceRepositoriesRequest));
    }

    /**
     * <p>
     * Retrieves a list of branches in a specified source repository.
     * </p>
     *
     * @param listSourceRepositoryBranchesRequest
     * @return Result of the ListSourceRepositoryBranches operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListSourceRepositoryBranches
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSourceRepositoryBranches"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public ListSourceRepositoryBranchesResponse listSourceRepositoryBranches(
            ListSourceRepositoryBranchesRequest listSourceRepositoryBranchesRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<ListSourceRepositoryBranchesResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, ListSourceRepositoryBranchesResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listSourceRepositoryBranchesRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListSourceRepositoryBranches");

            return clientHandler
                    .execute(new ClientExecutionParams<ListSourceRepositoryBranchesRequest, ListSourceRepositoryBranchesResponse>()
                            .withOperationName("ListSourceRepositoryBranches").withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                            .withInput(listSourceRepositoryBranchesRequest).withMetricCollector(apiCallMetricCollector)
                            .withMarshaller(new ListSourceRepositoryBranchesRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
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
     * operation. The return type is a custom iterable that can be used to iterate through all the pages. SDK will
     * internally handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoryBranchesIterable responses = client.listSourceRepositoryBranchesPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     * 
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoryBranchesIterable responses = client
     *             .listSourceRepositoryBranchesPaginator(request);
     *     for (software.amazon.awssdk.services.codecatalyst.model.ListSourceRepositoryBranchesResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSourceRepositoryBranchesIterable responses = client.listSourceRepositoryBranchesPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListSourceRepositoryBranches
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSourceRepositoryBranches"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public ListSourceRepositoryBranchesIterable listSourceRepositoryBranchesPaginator(
            ListSourceRepositoryBranchesRequest listSourceRepositoryBranchesRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return new ListSourceRepositoryBranchesIterable(this, applyPaginatorUserAgent(listSourceRepositoryBranchesRequest));
    }

    /**
     * <p>
     * Retrieves a list of spaces.
     * </p>
     *
     * @param listSpacesRequest
     * @return Result of the ListSpaces operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListSpaces
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSpaces" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public ListSpacesResponse listSpaces(ListSpacesRequest listSpacesRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<ListSpacesResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                ListSpacesResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listSpacesRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListSpaces");

            return clientHandler.execute(new ClientExecutionParams<ListSpacesRequest, ListSpacesResponse>()
                    .withOperationName("ListSpaces").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(listSpacesRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new ListSpacesRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Retrieves a list of spaces.
     * </p>
     * <br/>
     * <p>
     * This is a variant of {@link #listSpaces(software.amazon.awssdk.services.codecatalyst.model.ListSpacesRequest)}
     * operation. The return type is a custom iterable that can be used to iterate through all the pages. SDK will
     * internally handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSpacesIterable responses = client.listSpacesPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     * 
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.codecatalyst.paginators.ListSpacesIterable responses = client.listSpacesPaginator(request);
     *     for (software.amazon.awssdk.services.codecatalyst.model.ListSpacesResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.codecatalyst.paginators.ListSpacesIterable responses = client.listSpacesPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
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
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.ListSpaces
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/ListSpaces" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public ListSpacesIterable listSpacesPaginator(ListSpacesRequest listSpacesRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return new ListSpacesIterable(this, applyPaginatorUserAgent(listSpacesRequest));
    }

    /**
     * <p>
     * Starts a specified Dev Environment and puts it into an active state.
     * </p>
     *
     * @param startDevEnvironmentRequest
     * @return Result of the StartDevEnvironment operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.StartDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/StartDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StartDevEnvironmentResponse startDevEnvironment(StartDevEnvironmentRequest startDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<StartDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, StartDevEnvironmentResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, startDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StartDevEnvironment");

            return clientHandler.execute(new ClientExecutionParams<StartDevEnvironmentRequest, StartDevEnvironmentResponse>()
                    .withOperationName("StartDevEnvironment").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(startDevEnvironmentRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new StartDevEnvironmentRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Starts a session for a specified Dev Environment.
     * </p>
     *
     * @param startDevEnvironmentSessionRequest
     * @return Result of the StartDevEnvironmentSession operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.StartDevEnvironmentSession
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/StartDevEnvironmentSession"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StartDevEnvironmentSessionResponse startDevEnvironmentSession(
            StartDevEnvironmentSessionRequest startDevEnvironmentSessionRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<StartDevEnvironmentSessionResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, StartDevEnvironmentSessionResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, startDevEnvironmentSessionRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StartDevEnvironmentSession");

            return clientHandler
                    .execute(new ClientExecutionParams<StartDevEnvironmentSessionRequest, StartDevEnvironmentSessionResponse>()
                            .withOperationName("StartDevEnvironmentSession").withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                            .withInput(startDevEnvironmentSessionRequest).withMetricCollector(apiCallMetricCollector)
                            .withMarshaller(new StartDevEnvironmentSessionRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Pauses a specified Dev Environment and places it in a non-running state. Stopped Dev Environments do not consume
     * compute minutes.
     * </p>
     *
     * @param stopDevEnvironmentRequest
     * @return Result of the StopDevEnvironment operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.StopDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/StopDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StopDevEnvironmentResponse stopDevEnvironment(StopDevEnvironmentRequest stopDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<StopDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, StopDevEnvironmentResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, stopDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StopDevEnvironment");

            return clientHandler.execute(new ClientExecutionParams<StopDevEnvironmentRequest, StopDevEnvironmentResponse>()
                    .withOperationName("StopDevEnvironment").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(stopDevEnvironmentRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new StopDevEnvironmentRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Stops a session for a specified Dev Environment.
     * </p>
     *
     * @param stopDevEnvironmentSessionRequest
     * @return Result of the StopDevEnvironmentSession operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.StopDevEnvironmentSession
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/StopDevEnvironmentSession"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StopDevEnvironmentSessionResponse stopDevEnvironmentSession(
            StopDevEnvironmentSessionRequest stopDevEnvironmentSessionRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<StopDevEnvironmentSessionResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, StopDevEnvironmentSessionResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, stopDevEnvironmentSessionRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StopDevEnvironmentSession");

            return clientHandler
                    .execute(new ClientExecutionParams<StopDevEnvironmentSessionRequest, StopDevEnvironmentSessionResponse>()
                            .withOperationName("StopDevEnvironmentSession").withResponseHandler(responseHandler)
                            .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                            .withInput(stopDevEnvironmentSessionRequest).withMetricCollector(apiCallMetricCollector)
                            .withMarshaller(new StopDevEnvironmentSessionRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Changes one or more values for a Dev Environment. Updating certain values of the Dev Environment will cause a
     * restart.
     * </p>
     *
     * @param updateDevEnvironmentRequest
     * @return Result of the UpdateDevEnvironment operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.UpdateDevEnvironment
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/UpdateDevEnvironment"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public UpdateDevEnvironmentResponse updateDevEnvironment(UpdateDevEnvironmentRequest updateDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<UpdateDevEnvironmentResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, UpdateDevEnvironmentResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, updateDevEnvironmentRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "UpdateDevEnvironment");

            return clientHandler.execute(new ClientExecutionParams<UpdateDevEnvironmentRequest, UpdateDevEnvironmentResponse>()
                    .withOperationName("UpdateDevEnvironment").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(updateDevEnvironmentRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new UpdateDevEnvironmentRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Verifies whether the calling user has a valid Amazon CodeCatalyst login and session. If successful, this returns
     * the ID of the user in Amazon CodeCatalyst.
     * </p>
     *
     * @param verifySessionRequest
     * @return Result of the VerifySession operation returned by the service.
     * @throws ThrottlingException
     *         The request was denied due to request throttling.
     * @throws ConflictException
     *         The request was denied because the requested operation would cause a conflict with the current state of a
     *         service resource associated with the request. Another user might have updated the resource. Reload, make
     *         sure you have the latest data, and then try again.
     * @throws ValidationException
     *         The request was denied because an input failed to satisfy the constraints specified by the service. Check
     *         the spelling and input requirements, and then try again.
     * @throws ServiceQuotaExceededException
     *         The request was denied because one or more resources has reached its limits for the tier the space
     *         belongs to. Either reduce the number of resources, or change the tier if applicable.
     * @throws ResourceNotFoundException
     *         The request was denied because the specified resource was not found. Verify that the spelling is correct
     *         and that you have access to the resource.
     * @throws AccessDeniedException
     *         The request was denied because you don't have sufficient access to perform this action. Verify that you
     *         are a member of a role that allows this action.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws CodeCatalystException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample CodeCatalystClient.VerifySession
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/codecatalyst-2022-09-28/VerifySession" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public VerifySessionResponse verifySession(VerifySessionRequest verifySessionRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<VerifySessionResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                VerifySessionResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, verifySessionRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "CodeCatalyst");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "VerifySession");

            return clientHandler.execute(new ClientExecutionParams<VerifySessionRequest, VerifySessionResponse>()
                    .withOperationName("VerifySession").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                    .withInput(verifySessionRequest).withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new VerifySessionRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    private <T extends CodeCatalystRequest> T applyPaginatorUserAgent(T request) {
        Consumer<AwsRequestOverrideConfiguration.Builder> userAgentApplier = b -> b.addApiName(ApiName.builder()
                .version(VersionInfo.SDK_VERSION).name("PAGINATED").build());
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                .map(c -> c.toBuilder().applyMutation(userAgentApplier).build())
                .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
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

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
            JsonOperationMetadata operationMetadata) {
        return protocolFactory.createErrorResponseHandler(operationMetadata);
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

    @Override
    public final CodeCatalystServiceClientConfiguration serviceClientConfiguration() {
        return this.serviceClientConfiguration;
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}

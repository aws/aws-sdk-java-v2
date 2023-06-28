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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.codecatalyst.model.AccessDeniedException;
import software.amazon.awssdk.services.codecatalyst.model.CodeCatalystException;
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

/**
 * Service client for accessing Amazon CodeCatalyst. This can be created using the static {@link #builder()} method.
 *
 * <p>
 * Welcome to the Amazon CodeCatalyst API reference. This reference provides descriptions of operations and data types
 * for Amazon CodeCatalyst. You can use the Amazon CodeCatalyst API to work with the following objects.
 * </p>
 * <p>
 * Dev Environments and the Amazon Web Services Toolkits, by calling the following:
 * </p>
 * <ul>
 * <li>
 * <p>
 * <a>CreateAccessToken</a>, which creates a personal access token (PAT) for the current user.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>CreateDevEnvironment</a>, which creates a Dev Environment, where you can quickly work on the code stored in the
 * source repositories of your project.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>CreateProject</a> which creates a project in a specified space.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>CreateSourceRepositoryBranch</a>, which creates a branch in a specified repository where you can work on code.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>DeleteDevEnvironment</a>, which deletes a Dev Environment.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>GetDevEnvironment</a>, which returns information about a Dev Environment.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>GetProject</a>, which returns information about a project.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>GetSourceRepositoryCloneUrls</a>, which returns information about the URLs that can be used with a Git client to
 * clone a source repository.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>GetSpace</a>, which returns information about a space.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>GetSubscription</a>, which returns information about the Amazon Web Services account used for billing purposes and
 * the billing plan for the space.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>GetUserDetails</a>, which returns information about a user in Amazon CodeCatalyst.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>ListDevEnvironments</a>, which retrieves a list of Dev Environments in a project.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>ListDevEnvironmentSessions</a>, which retrieves a list of active Dev Environment sessions in a project.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>ListProjects</a>, which retrieves a list of projects in a space.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>ListSourceRepositories</a>, which retrieves a list of source repositories in a project.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>ListSourceRepositoryBranches</a>, which retrieves a list of branches in a source repository.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>ListSpaces</a>, which retrieves a list of spaces.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>StartDevEnvironment</a>, which starts a specified Dev Environment and puts it into an active state.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>StartDevEnvironmentSession</a>, which starts a session to a specified Dev Environment.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>StopDevEnvironment</a>, which stops a specified Dev Environment and puts it into an stopped state.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>StopDevEnvironmentSession</a>, which stops a session for a specified Dev Environment.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>UpdateDevEnvironment</a>, which changes one or more values for a Dev Environment.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>VerifySession</a>, which verifies whether the calling user has a valid Amazon CodeCatalyst login and session.
 * </p>
 * </li>
 * </ul>
 * <p>
 * Security, activity, and resource management in Amazon CodeCatalyst, by calling the following:
 * </p>
 * <ul>
 * <li>
 * <p>
 * <a>DeleteAccessToken</a>, which deletes a specified personal access token (PAT).
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>ListAccessTokens</a>, which lists all personal access tokens (PATs) associated with a user.
 * </p>
 * </li>
 * <li>
 * <p>
 * <a>ListEventLogs</a>, which retrieves a list of events that occurred during a specified time period in a space.
 * </p>
 * </li>
 * </ul>
 * <note>
 * <p>
 * If you are using the Amazon CodeCatalyst APIs with an SDK or the CLI, you must configure your computer to work with
 * Amazon CodeCatalyst and single sign-on (SSO). For more information, see <a
 * href="https://docs.aws.amazon.com/codecatalyst/latest/userguide/set-up-cli.html">Setting up to use the CLI with
 * Amazon CodeCatalyst</a> and the SSO documentation for your SDK.
 * </p>
 * </note>
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface CodeCatalystClient extends AwsClient {
    String SERVICE_NAME = "codecatalyst";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "codecatalyst";

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
    default CreateAccessTokenResponse createAccessToken(CreateAccessTokenRequest createAccessTokenRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link CreateAccessTokenRequest.Builder} avoiding the need
     * to create one manually via {@link CreateAccessTokenRequest#builder()}
     * </p>
     *
     * @param createAccessTokenRequest
     *        A {@link Consumer} that will call methods on {@link CreateAccessTokenRequest.Builder} to create a request.
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
    default CreateAccessTokenResponse createAccessToken(Consumer<CreateAccessTokenRequest.Builder> createAccessTokenRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return createAccessToken(CreateAccessTokenRequest.builder().applyMutation(createAccessTokenRequest).build());
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
    default CreateDevEnvironmentResponse createDevEnvironment(CreateDevEnvironmentRequest createDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link CreateDevEnvironmentRequest.Builder} avoiding the
     * need to create one manually via {@link CreateDevEnvironmentRequest#builder()}
     * </p>
     *
     * @param createDevEnvironmentRequest
     *        A {@link Consumer} that will call methods on {@link CreateDevEnvironmentRequest.Builder} to create a
     *        request.
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
    default CreateDevEnvironmentResponse createDevEnvironment(
            Consumer<CreateDevEnvironmentRequest.Builder> createDevEnvironmentRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return createDevEnvironment(CreateDevEnvironmentRequest.builder().applyMutation(createDevEnvironmentRequest).build());
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
    default CreateProjectResponse createProject(CreateProjectRequest createProjectRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Creates a project in a specified space.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link CreateProjectRequest.Builder} avoiding the need to
     * create one manually via {@link CreateProjectRequest#builder()}
     * </p>
     *
     * @param createProjectRequest
     *        A {@link Consumer} that will call methods on {@link CreateProjectRequest.Builder} to create a request.
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
    default CreateProjectResponse createProject(Consumer<CreateProjectRequest.Builder> createProjectRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return createProject(CreateProjectRequest.builder().applyMutation(createProjectRequest).build());
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
    default CreateSourceRepositoryBranchResponse createSourceRepositoryBranch(
            CreateSourceRepositoryBranchRequest createSourceRepositoryBranchRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * </note><br/>
     * <p>
     * This is a convenience which creates an instance of the {@link CreateSourceRepositoryBranchRequest.Builder}
     * avoiding the need to create one manually via {@link CreateSourceRepositoryBranchRequest#builder()}
     * </p>
     *
     * @param createSourceRepositoryBranchRequest
     *        A {@link Consumer} that will call methods on {@link CreateSourceRepositoryBranchRequest.Builder} to create
     *        a request.
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
    default CreateSourceRepositoryBranchResponse createSourceRepositoryBranch(
            Consumer<CreateSourceRepositoryBranchRequest.Builder> createSourceRepositoryBranchRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return createSourceRepositoryBranch(CreateSourceRepositoryBranchRequest.builder()
                .applyMutation(createSourceRepositoryBranchRequest).build());
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
    default DeleteAccessTokenResponse deleteAccessToken(DeleteAccessTokenRequest deleteAccessTokenRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Deletes a specified personal access token (PAT). A personal access token can only be deleted by the user who
     * created it.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteAccessTokenRequest.Builder} avoiding the need
     * to create one manually via {@link DeleteAccessTokenRequest#builder()}
     * </p>
     *
     * @param deleteAccessTokenRequest
     *        A {@link Consumer} that will call methods on {@link DeleteAccessTokenRequest.Builder} to create a request.
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
    default DeleteAccessTokenResponse deleteAccessToken(Consumer<DeleteAccessTokenRequest.Builder> deleteAccessTokenRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return deleteAccessToken(DeleteAccessTokenRequest.builder().applyMutation(deleteAccessTokenRequest).build());
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
    default DeleteDevEnvironmentResponse deleteDevEnvironment(DeleteDevEnvironmentRequest deleteDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Deletes a Dev Environment.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteDevEnvironmentRequest.Builder} avoiding the
     * need to create one manually via {@link DeleteDevEnvironmentRequest#builder()}
     * </p>
     *
     * @param deleteDevEnvironmentRequest
     *        A {@link Consumer} that will call methods on {@link DeleteDevEnvironmentRequest.Builder} to create a
     *        request.
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
    default DeleteDevEnvironmentResponse deleteDevEnvironment(
            Consumer<DeleteDevEnvironmentRequest.Builder> deleteDevEnvironmentRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return deleteDevEnvironment(DeleteDevEnvironmentRequest.builder().applyMutation(deleteDevEnvironmentRequest).build());
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
    default GetDevEnvironmentResponse getDevEnvironment(GetDevEnvironmentRequest getDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Returns information about a Dev Environment for a source repository in a project. Dev Environments are specific
     * to the user who creates them.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetDevEnvironmentRequest.Builder} avoiding the need
     * to create one manually via {@link GetDevEnvironmentRequest#builder()}
     * </p>
     *
     * @param getDevEnvironmentRequest
     *        A {@link Consumer} that will call methods on {@link GetDevEnvironmentRequest.Builder} to create a request.
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
    default GetDevEnvironmentResponse getDevEnvironment(Consumer<GetDevEnvironmentRequest.Builder> getDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return getDevEnvironment(GetDevEnvironmentRequest.builder().applyMutation(getDevEnvironmentRequest).build());
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
    default GetProjectResponse getProject(GetProjectRequest getProjectRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Returns information about a project.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetProjectRequest.Builder} avoiding the need to
     * create one manually via {@link GetProjectRequest#builder()}
     * </p>
     *
     * @param getProjectRequest
     *        A {@link Consumer} that will call methods on {@link GetProjectRequest.Builder} to create a request.
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
    default GetProjectResponse getProject(Consumer<GetProjectRequest.Builder> getProjectRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return getProject(GetProjectRequest.builder().applyMutation(getProjectRequest).build());
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
    default GetSourceRepositoryCloneUrlsResponse getSourceRepositoryCloneUrls(
            GetSourceRepositoryCloneUrlsRequest getSourceRepositoryCloneUrlsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Returns information about the URLs that can be used with a Git client to clone a source repository.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetSourceRepositoryCloneUrlsRequest.Builder}
     * avoiding the need to create one manually via {@link GetSourceRepositoryCloneUrlsRequest#builder()}
     * </p>
     *
     * @param getSourceRepositoryCloneUrlsRequest
     *        A {@link Consumer} that will call methods on {@link GetSourceRepositoryCloneUrlsRequest.Builder} to create
     *        a request.
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
    default GetSourceRepositoryCloneUrlsResponse getSourceRepositoryCloneUrls(
            Consumer<GetSourceRepositoryCloneUrlsRequest.Builder> getSourceRepositoryCloneUrlsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return getSourceRepositoryCloneUrls(GetSourceRepositoryCloneUrlsRequest.builder()
                .applyMutation(getSourceRepositoryCloneUrlsRequest).build());
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
    default GetSpaceResponse getSpace(GetSpaceRequest getSpaceRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Returns information about an space.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetSpaceRequest.Builder} avoiding the need to
     * create one manually via {@link GetSpaceRequest#builder()}
     * </p>
     *
     * @param getSpaceRequest
     *        A {@link Consumer} that will call methods on {@link GetSpaceRequest.Builder} to create a request.
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
    default GetSpaceResponse getSpace(Consumer<GetSpaceRequest.Builder> getSpaceRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return getSpace(GetSpaceRequest.builder().applyMutation(getSpaceRequest).build());
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
    default GetSubscriptionResponse getSubscription(GetSubscriptionRequest getSubscriptionRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Returns information about the Amazon Web Services account used for billing purposes and the billing plan for the
     * space.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetSubscriptionRequest.Builder} avoiding the need
     * to create one manually via {@link GetSubscriptionRequest#builder()}
     * </p>
     *
     * @param getSubscriptionRequest
     *        A {@link Consumer} that will call methods on {@link GetSubscriptionRequest.Builder} to create a request.
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
    default GetSubscriptionResponse getSubscription(Consumer<GetSubscriptionRequest.Builder> getSubscriptionRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return getSubscription(GetSubscriptionRequest.builder().applyMutation(getSubscriptionRequest).build());
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
    default GetUserDetailsResponse getUserDetails(GetUserDetailsRequest getUserDetailsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Returns information about a user.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetUserDetailsRequest.Builder} avoiding the need to
     * create one manually via {@link GetUserDetailsRequest#builder()}
     * </p>
     *
     * @param getUserDetailsRequest
     *        A {@link Consumer} that will call methods on {@link GetUserDetailsRequest.Builder} to create a request.
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
    default GetUserDetailsResponse getUserDetails(Consumer<GetUserDetailsRequest.Builder> getUserDetailsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return getUserDetails(GetUserDetailsRequest.builder().applyMutation(getUserDetailsRequest).build());
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
    default ListAccessTokensResponse listAccessTokens(ListAccessTokensRequest listAccessTokensRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Lists all personal access tokens (PATs) associated with the user who calls the API. You can only list PATs
     * associated with your Amazon Web Services Builder ID.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListAccessTokensRequest.Builder} avoiding the need
     * to create one manually via {@link ListAccessTokensRequest#builder()}
     * </p>
     *
     * @param listAccessTokensRequest
     *        A {@link Consumer} that will call methods on {@link ListAccessTokensRequest.Builder} to create a request.
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
    default ListAccessTokensResponse listAccessTokens(Consumer<ListAccessTokensRequest.Builder> listAccessTokensRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listAccessTokens(ListAccessTokensRequest.builder().applyMutation(listAccessTokensRequest).build());
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
    default ListAccessTokensIterable listAccessTokensPaginator(ListAccessTokensRequest listAccessTokensRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * <p>
     * This is a convenience which creates an instance of the {@link ListAccessTokensRequest.Builder} avoiding the need
     * to create one manually via {@link ListAccessTokensRequest#builder()}
     * </p>
     *
     * @param listAccessTokensRequest
     *        A {@link Consumer} that will call methods on {@link ListAccessTokensRequest.Builder} to create a request.
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
    default ListAccessTokensIterable listAccessTokensPaginator(Consumer<ListAccessTokensRequest.Builder> listAccessTokensRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listAccessTokensPaginator(ListAccessTokensRequest.builder().applyMutation(listAccessTokensRequest).build());
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
    default ListDevEnvironmentSessionsResponse listDevEnvironmentSessions(
            ListDevEnvironmentSessionsRequest listDevEnvironmentSessionsRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves a list of active sessions for a Dev Environment in a project.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListDevEnvironmentSessionsRequest.Builder} avoiding
     * the need to create one manually via {@link ListDevEnvironmentSessionsRequest#builder()}
     * </p>
     *
     * @param listDevEnvironmentSessionsRequest
     *        A {@link Consumer} that will call methods on {@link ListDevEnvironmentSessionsRequest.Builder} to create a
     *        request.
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
    default ListDevEnvironmentSessionsResponse listDevEnvironmentSessions(
            Consumer<ListDevEnvironmentSessionsRequest.Builder> listDevEnvironmentSessionsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listDevEnvironmentSessions(ListDevEnvironmentSessionsRequest.builder()
                .applyMutation(listDevEnvironmentSessionsRequest).build());
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
    default ListDevEnvironmentSessionsIterable listDevEnvironmentSessionsPaginator(
            ListDevEnvironmentSessionsRequest listDevEnvironmentSessionsRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * <p>
     * This is a convenience which creates an instance of the {@link ListDevEnvironmentSessionsRequest.Builder} avoiding
     * the need to create one manually via {@link ListDevEnvironmentSessionsRequest#builder()}
     * </p>
     *
     * @param listDevEnvironmentSessionsRequest
     *        A {@link Consumer} that will call methods on {@link ListDevEnvironmentSessionsRequest.Builder} to create a
     *        request.
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
    default ListDevEnvironmentSessionsIterable listDevEnvironmentSessionsPaginator(
            Consumer<ListDevEnvironmentSessionsRequest.Builder> listDevEnvironmentSessionsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listDevEnvironmentSessionsPaginator(ListDevEnvironmentSessionsRequest.builder()
                .applyMutation(listDevEnvironmentSessionsRequest).build());
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
    default ListDevEnvironmentsResponse listDevEnvironments(ListDevEnvironmentsRequest listDevEnvironmentsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves a list of Dev Environments in a project.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListDevEnvironmentsRequest.Builder} avoiding the
     * need to create one manually via {@link ListDevEnvironmentsRequest#builder()}
     * </p>
     *
     * @param listDevEnvironmentsRequest
     *        A {@link Consumer} that will call methods on {@link ListDevEnvironmentsRequest.Builder} to create a
     *        request.
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
    default ListDevEnvironmentsResponse listDevEnvironments(
            Consumer<ListDevEnvironmentsRequest.Builder> listDevEnvironmentsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listDevEnvironments(ListDevEnvironmentsRequest.builder().applyMutation(listDevEnvironmentsRequest).build());
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
    default ListDevEnvironmentsIterable listDevEnvironmentsPaginator(ListDevEnvironmentsRequest listDevEnvironmentsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * <p>
     * This is a convenience which creates an instance of the {@link ListDevEnvironmentsRequest.Builder} avoiding the
     * need to create one manually via {@link ListDevEnvironmentsRequest#builder()}
     * </p>
     *
     * @param listDevEnvironmentsRequest
     *        A {@link Consumer} that will call methods on {@link ListDevEnvironmentsRequest.Builder} to create a
     *        request.
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
    default ListDevEnvironmentsIterable listDevEnvironmentsPaginator(
            Consumer<ListDevEnvironmentsRequest.Builder> listDevEnvironmentsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listDevEnvironmentsPaginator(ListDevEnvironmentsRequest.builder().applyMutation(listDevEnvironmentsRequest)
                .build());
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
    default ListEventLogsResponse listEventLogs(ListEventLogsRequest listEventLogsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves a list of events that occurred during a specified time period in a space. You can use these events to
     * audit user and system activity in a space.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListEventLogsRequest.Builder} avoiding the need to
     * create one manually via {@link ListEventLogsRequest#builder()}
     * </p>
     *
     * @param listEventLogsRequest
     *        A {@link Consumer} that will call methods on {@link ListEventLogsRequest.Builder} to create a request.
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
    default ListEventLogsResponse listEventLogs(Consumer<ListEventLogsRequest.Builder> listEventLogsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listEventLogs(ListEventLogsRequest.builder().applyMutation(listEventLogsRequest).build());
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
    default ListEventLogsIterable listEventLogsPaginator(ListEventLogsRequest listEventLogsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * <p>
     * This is a convenience which creates an instance of the {@link ListEventLogsRequest.Builder} avoiding the need to
     * create one manually via {@link ListEventLogsRequest#builder()}
     * </p>
     *
     * @param listEventLogsRequest
     *        A {@link Consumer} that will call methods on {@link ListEventLogsRequest.Builder} to create a request.
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
    default ListEventLogsIterable listEventLogsPaginator(Consumer<ListEventLogsRequest.Builder> listEventLogsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listEventLogsPaginator(ListEventLogsRequest.builder().applyMutation(listEventLogsRequest).build());
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
    default ListProjectsResponse listProjects(ListProjectsRequest listProjectsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves a list of projects.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListProjectsRequest.Builder} avoiding the need to
     * create one manually via {@link ListProjectsRequest#builder()}
     * </p>
     *
     * @param listProjectsRequest
     *        A {@link Consumer} that will call methods on {@link ListProjectsRequest.Builder} to create a request.
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
    default ListProjectsResponse listProjects(Consumer<ListProjectsRequest.Builder> listProjectsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listProjects(ListProjectsRequest.builder().applyMutation(listProjectsRequest).build());
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
    default ListProjectsIterable listProjectsPaginator(ListProjectsRequest listProjectsRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * <p>
     * This is a convenience which creates an instance of the {@link ListProjectsRequest.Builder} avoiding the need to
     * create one manually via {@link ListProjectsRequest#builder()}
     * </p>
     *
     * @param listProjectsRequest
     *        A {@link Consumer} that will call methods on {@link ListProjectsRequest.Builder} to create a request.
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
    default ListProjectsIterable listProjectsPaginator(Consumer<ListProjectsRequest.Builder> listProjectsRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listProjectsPaginator(ListProjectsRequest.builder().applyMutation(listProjectsRequest).build());
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
    default ListSourceRepositoriesResponse listSourceRepositories(ListSourceRepositoriesRequest listSourceRepositoriesRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves a list of source repositories in a project.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListSourceRepositoriesRequest.Builder} avoiding the
     * need to create one manually via {@link ListSourceRepositoriesRequest#builder()}
     * </p>
     *
     * @param listSourceRepositoriesRequest
     *        A {@link Consumer} that will call methods on {@link ListSourceRepositoriesRequest.Builder} to create a
     *        request.
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
    default ListSourceRepositoriesResponse listSourceRepositories(
            Consumer<ListSourceRepositoriesRequest.Builder> listSourceRepositoriesRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listSourceRepositories(ListSourceRepositoriesRequest.builder().applyMutation(listSourceRepositoriesRequest)
                .build());
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
    default ListSourceRepositoriesIterable listSourceRepositoriesPaginator(
            ListSourceRepositoriesRequest listSourceRepositoriesRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * <p>
     * This is a convenience which creates an instance of the {@link ListSourceRepositoriesRequest.Builder} avoiding the
     * need to create one manually via {@link ListSourceRepositoriesRequest#builder()}
     * </p>
     *
     * @param listSourceRepositoriesRequest
     *        A {@link Consumer} that will call methods on {@link ListSourceRepositoriesRequest.Builder} to create a
     *        request.
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
    default ListSourceRepositoriesIterable listSourceRepositoriesPaginator(
            Consumer<ListSourceRepositoriesRequest.Builder> listSourceRepositoriesRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listSourceRepositoriesPaginator(ListSourceRepositoriesRequest.builder()
                .applyMutation(listSourceRepositoriesRequest).build());
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
    default ListSourceRepositoryBranchesResponse listSourceRepositoryBranches(
            ListSourceRepositoryBranchesRequest listSourceRepositoryBranchesRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves a list of branches in a specified source repository.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListSourceRepositoryBranchesRequest.Builder}
     * avoiding the need to create one manually via {@link ListSourceRepositoryBranchesRequest#builder()}
     * </p>
     *
     * @param listSourceRepositoryBranchesRequest
     *        A {@link Consumer} that will call methods on {@link ListSourceRepositoryBranchesRequest.Builder} to create
     *        a request.
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
    default ListSourceRepositoryBranchesResponse listSourceRepositoryBranches(
            Consumer<ListSourceRepositoryBranchesRequest.Builder> listSourceRepositoryBranchesRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listSourceRepositoryBranches(ListSourceRepositoryBranchesRequest.builder()
                .applyMutation(listSourceRepositoryBranchesRequest).build());
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
    default ListSourceRepositoryBranchesIterable listSourceRepositoryBranchesPaginator(
            ListSourceRepositoryBranchesRequest listSourceRepositoryBranchesRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * <p>
     * This is a convenience which creates an instance of the {@link ListSourceRepositoryBranchesRequest.Builder}
     * avoiding the need to create one manually via {@link ListSourceRepositoryBranchesRequest#builder()}
     * </p>
     *
     * @param listSourceRepositoryBranchesRequest
     *        A {@link Consumer} that will call methods on {@link ListSourceRepositoryBranchesRequest.Builder} to create
     *        a request.
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
    default ListSourceRepositoryBranchesIterable listSourceRepositoryBranchesPaginator(
            Consumer<ListSourceRepositoryBranchesRequest.Builder> listSourceRepositoryBranchesRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listSourceRepositoryBranchesPaginator(ListSourceRepositoryBranchesRequest.builder()
                .applyMutation(listSourceRepositoryBranchesRequest).build());
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
    default ListSpacesResponse listSpaces(ListSpacesRequest listSpacesRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Retrieves a list of spaces.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link ListSpacesRequest.Builder} avoiding the need to
     * create one manually via {@link ListSpacesRequest#builder()}
     * </p>
     *
     * @param listSpacesRequest
     *        A {@link Consumer} that will call methods on {@link ListSpacesRequest.Builder} to create a request.
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
    default ListSpacesResponse listSpaces(Consumer<ListSpacesRequest.Builder> listSpacesRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listSpaces(ListSpacesRequest.builder().applyMutation(listSpacesRequest).build());
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
    default ListSpacesIterable listSpacesPaginator(ListSpacesRequest listSpacesRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
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
     * <p>
     * This is a convenience which creates an instance of the {@link ListSpacesRequest.Builder} avoiding the need to
     * create one manually via {@link ListSpacesRequest#builder()}
     * </p>
     *
     * @param listSpacesRequest
     *        A {@link Consumer} that will call methods on {@link ListSpacesRequest.Builder} to create a request.
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
    default ListSpacesIterable listSpacesPaginator(Consumer<ListSpacesRequest.Builder> listSpacesRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return listSpacesPaginator(ListSpacesRequest.builder().applyMutation(listSpacesRequest).build());
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
    default StartDevEnvironmentResponse startDevEnvironment(StartDevEnvironmentRequest startDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Starts a specified Dev Environment and puts it into an active state.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StartDevEnvironmentRequest.Builder} avoiding the
     * need to create one manually via {@link StartDevEnvironmentRequest#builder()}
     * </p>
     *
     * @param startDevEnvironmentRequest
     *        A {@link Consumer} that will call methods on {@link StartDevEnvironmentRequest.Builder} to create a
     *        request.
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
    default StartDevEnvironmentResponse startDevEnvironment(
            Consumer<StartDevEnvironmentRequest.Builder> startDevEnvironmentRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return startDevEnvironment(StartDevEnvironmentRequest.builder().applyMutation(startDevEnvironmentRequest).build());
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
    default StartDevEnvironmentSessionResponse startDevEnvironmentSession(
            StartDevEnvironmentSessionRequest startDevEnvironmentSessionRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Starts a session for a specified Dev Environment.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StartDevEnvironmentSessionRequest.Builder} avoiding
     * the need to create one manually via {@link StartDevEnvironmentSessionRequest#builder()}
     * </p>
     *
     * @param startDevEnvironmentSessionRequest
     *        A {@link Consumer} that will call methods on {@link StartDevEnvironmentSessionRequest.Builder} to create a
     *        request.
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
    default StartDevEnvironmentSessionResponse startDevEnvironmentSession(
            Consumer<StartDevEnvironmentSessionRequest.Builder> startDevEnvironmentSessionRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return startDevEnvironmentSession(StartDevEnvironmentSessionRequest.builder()
                .applyMutation(startDevEnvironmentSessionRequest).build());
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
    default StopDevEnvironmentResponse stopDevEnvironment(StopDevEnvironmentRequest stopDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Pauses a specified Dev Environment and places it in a non-running state. Stopped Dev Environments do not consume
     * compute minutes.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StopDevEnvironmentRequest.Builder} avoiding the
     * need to create one manually via {@link StopDevEnvironmentRequest#builder()}
     * </p>
     *
     * @param stopDevEnvironmentRequest
     *        A {@link Consumer} that will call methods on {@link StopDevEnvironmentRequest.Builder} to create a
     *        request.
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
    default StopDevEnvironmentResponse stopDevEnvironment(Consumer<StopDevEnvironmentRequest.Builder> stopDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return stopDevEnvironment(StopDevEnvironmentRequest.builder().applyMutation(stopDevEnvironmentRequest).build());
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
    default StopDevEnvironmentSessionResponse stopDevEnvironmentSession(
            StopDevEnvironmentSessionRequest stopDevEnvironmentSessionRequest) throws ThrottlingException, ConflictException,
            ValidationException, ServiceQuotaExceededException, ResourceNotFoundException, AccessDeniedException,
            AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Stops a session for a specified Dev Environment.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StopDevEnvironmentSessionRequest.Builder} avoiding
     * the need to create one manually via {@link StopDevEnvironmentSessionRequest#builder()}
     * </p>
     *
     * @param stopDevEnvironmentSessionRequest
     *        A {@link Consumer} that will call methods on {@link StopDevEnvironmentSessionRequest.Builder} to create a
     *        request.
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
    default StopDevEnvironmentSessionResponse stopDevEnvironmentSession(
            Consumer<StopDevEnvironmentSessionRequest.Builder> stopDevEnvironmentSessionRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return stopDevEnvironmentSession(StopDevEnvironmentSessionRequest.builder()
                .applyMutation(stopDevEnvironmentSessionRequest).build());
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
    default UpdateDevEnvironmentResponse updateDevEnvironment(UpdateDevEnvironmentRequest updateDevEnvironmentRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Changes one or more values for a Dev Environment. Updating certain values of the Dev Environment will cause a
     * restart.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link UpdateDevEnvironmentRequest.Builder} avoiding the
     * need to create one manually via {@link UpdateDevEnvironmentRequest#builder()}
     * </p>
     *
     * @param updateDevEnvironmentRequest
     *        A {@link Consumer} that will call methods on {@link UpdateDevEnvironmentRequest.Builder} to create a
     *        request.
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
    default UpdateDevEnvironmentResponse updateDevEnvironment(
            Consumer<UpdateDevEnvironmentRequest.Builder> updateDevEnvironmentRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return updateDevEnvironment(UpdateDevEnvironmentRequest.builder().applyMutation(updateDevEnvironmentRequest).build());
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
    default VerifySessionResponse verifySession(VerifySessionRequest verifySessionRequest) throws ThrottlingException,
            ConflictException, ValidationException, ServiceQuotaExceededException, ResourceNotFoundException,
            AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Verifies whether the calling user has a valid Amazon CodeCatalyst login and session. If successful, this returns
     * the ID of the user in Amazon CodeCatalyst.
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link VerifySessionRequest.Builder} avoiding the need to
     * create one manually via {@link VerifySessionRequest#builder()}
     * </p>
     *
     * @param verifySessionRequest
     *        A {@link Consumer} that will call methods on {@link VerifySessionRequest.Builder} to create a request.
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
    default VerifySessionResponse verifySession(Consumer<VerifySessionRequest.Builder> verifySessionRequest)
            throws ThrottlingException, ConflictException, ValidationException, ServiceQuotaExceededException,
            ResourceNotFoundException, AccessDeniedException, AwsServiceException, SdkClientException, CodeCatalystException {
        return verifySession(VerifySessionRequest.builder().applyMutation(verifySessionRequest).build());
    }

    /**
     * Create a {@link CodeCatalystClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static CodeCatalystClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link CodeCatalystClient}.
     */
    static CodeCatalystClientBuilder builder() {
        return new DefaultCodeCatalystClientBuilder();
    }

    static ServiceMetadata serviceMetadata() {
        return ServiceMetadata.of(SERVICE_METADATA_ID);
    }

    @Override
    default CodeCatalystServiceClientConfiguration serviceClientConfiguration() {
        throw new UnsupportedOperationException();
    }
}

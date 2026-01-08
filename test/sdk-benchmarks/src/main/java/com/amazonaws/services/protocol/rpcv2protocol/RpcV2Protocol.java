/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.services.protocol.rpcv2protocol;



import com.amazonaws.*;
import com.amazonaws.regions.*;

import com.amazonaws.services.protocol.rpcv2protocol.model.*;

/**
 * Interface for accessing RpcV2Protocol.
 * <p>
 * <b>Note:</b> Do not directly implement this interface, new methods are added to it regularly. Extend from
 * {@link AbstractRpcV2Protocol} instead.
 * </p>
 */

public interface RpcV2Protocol {

    /**
     * The region metadata service name for computing region endpoints. You can use this value to retrieve metadata
     * (such as supported regions) of the service.
     *
     * @see RegionUtils#getRegionsForService(String)
     */
    String ENDPOINT_PREFIX = "rpcv2protocol";

    /**
     * Overrides the default endpoint for this client ("https://protocol-rpcv2protocol.us-east-1.amazonaws.com").
     * Callers can use this method to control which AWS region they want to work with.
     * <p>
     * Callers can pass in just the endpoint (ex: "protocol-rpcv2protocol.us-east-1.amazonaws.com") or a full URL,
     * including the protocol (ex: "https://protocol-rpcv2protocol.us-east-1.amazonaws.com"). If the protocol is not
     * specified here, the default protocol from this client's {@link ClientConfiguration} will be used, which by
     * default is HTTPS.
     * <p>
     * For more information on using AWS regions with the AWS SDK for Java, and a complete list of all available
     * endpoints for all AWS services, see: <a href=
     * "https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html#region-selection-choose-endpoint"
     * > https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html#region-selection-
     * choose-endpoint</a>
     * <p>
     * <b>This method is not threadsafe. An endpoint should be configured when the client is created and before any
     * service requests are made. Changing it afterwards creates inevitable race conditions for any service requests in
     * transit or retrying.</b>
     *
     * @param endpoint
     *        The endpoint (ex: "protocol-rpcv2protocol.us-east-1.amazonaws.com") or a full URL, including the protocol
     *        (ex: "https://protocol-rpcv2protocol.us-east-1.amazonaws.com") of the region specific AWS endpoint this
     *        client will communicate with.
     * @deprecated use {@link AwsClientBuilder#setEndpointConfiguration(AwsClientBuilder.EndpointConfiguration)} for
     *             example:
     *             {@code builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, signingRegion));}
     */
    @Deprecated
    void setEndpoint(String endpoint);

    /**
     * An alternative to {@link RpcV2Protocol#setEndpoint(String)}, sets the regional endpoint for this client's service
     * calls. Callers can use this method to control which AWS region they want to work with.
     * <p>
     * By default, all service endpoints in all regions use the https protocol. To use http instead, specify it in the
     * {@link ClientConfiguration} supplied at construction.
     * <p>
     * <b>This method is not threadsafe. A region should be configured when the client is created and before any service
     * requests are made. Changing it afterwards creates inevitable race conditions for any service requests in transit
     * or retrying.</b>
     *
     * @param region
     *        The region this client will communicate with. See {@link Region#getRegion(Regions)}
     *        for accessing a given region. Must not be null and must be a region where the service is available.
     *
     * @see Region#getRegion(Regions)
     * @see Region#createClient(Class, com.amazonaws.auth.AWSCredentialsProvider, ClientConfiguration)
     * @see Region#isServiceSupported(String)
     * @deprecated use {@link AwsClientBuilder#setRegion(String)}
     */
    @Deprecated
    void setRegion(Region region);

    /**
     * @param emptyInputOutputRequest
     * @return Result of the EmptyInputOutput operation returned by the service.
     * @sample RpcV2Protocol.EmptyInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/EmptyInputOutput" target="_top">AWS
     *      API Documentation</a>
     */
    EmptyInputOutputResult emptyInputOutput(EmptyInputOutputRequest emptyInputOutputRequest);

    /**
     * @param float16Request
     * @return Result of the Float16 operation returned by the service.
     * @sample RpcV2Protocol.Float16
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/Float16" target="_top">AWS API
     *      Documentation</a>
     */
    Float16Result float16(Float16Request float16Request);

    /**
     * @param fractionalSecondsRequest
     * @return Result of the FractionalSeconds operation returned by the service.
     * @sample RpcV2Protocol.FractionalSeconds
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/FractionalSeconds"
     *      target="_top">AWS API Documentation</a>
     */
    FractionalSecondsResult fractionalSeconds(FractionalSecondsRequest fractionalSecondsRequest);

    /**
     * <p>
     * This operation has three possible return values:
     * </p>
     * <ol>
     * <li>A successful response in the form of GreetingWithErrorsOutput</li>
     * <li>An InvalidGreeting error.</li>
     * <li>A ComplexError error.</li>
     * </ol>
     * <p>
     * Implementations must be able to successfully take a response and properly deserialize successful and error
     * responses.
     * </p>
     * 
     * @param greetingWithErrorsRequest
     * @return Result of the GreetingWithErrors operation returned by the service.
     * @throws ComplexErrorException
     *         This error is thrown when a request is invalid.
     * @throws InvalidGreetingException
     *         This error is thrown when an invalid greeting value is provided.
     * @sample RpcV2Protocol.GreetingWithErrors
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/GreetingWithErrors"
     *      target="_top">AWS API Documentation</a>
     */
    GreetingWithErrorsResult greetingWithErrors(GreetingWithErrorsRequest greetingWithErrorsRequest);

    /**
     * @param noInputOutputRequest
     * @return Result of the NoInputOutput operation returned by the service.
     * @sample RpcV2Protocol.NoInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/NoInputOutput" target="_top">AWS
     *      API Documentation</a>
     */
    NoInputOutputResult noInputOutput(NoInputOutputRequest noInputOutputRequest);

    /**
     * @param optionalInputOutputRequest
     * @return Result of the OptionalInputOutput operation returned by the service.
     * @sample RpcV2Protocol.OptionalInputOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/OptionalInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    OptionalInputOutputResult optionalInputOutput(OptionalInputOutputRequest optionalInputOutputRequest);

    /**
     * @param recursiveShapesRequest
     * @return Result of the RecursiveShapes operation returned by the service.
     * @sample RpcV2Protocol.RecursiveShapes
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RecursiveShapes" target="_top">AWS
     *      API Documentation</a>
     */
    RecursiveShapesResult recursiveShapes(RecursiveShapesRequest recursiveShapesRequest);

    /**
     * <p>
     * The example tests basic map serialization.
     * </p>
     * 
     * @param rpcV2CborDenseMapsRequest
     * @return Result of the RpcV2CborDenseMaps operation returned by the service.
     * @throws ValidationException
     *         A standard error for input validation failures. This should be thrown by services when a member of the
     *         input structure falls outside of the modeled or documented constraints.
     * @sample RpcV2Protocol.RpcV2CborDenseMaps
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborDenseMaps"
     *      target="_top">AWS API Documentation</a>
     */
    RpcV2CborDenseMapsResult rpcV2CborDenseMaps(RpcV2CborDenseMapsRequest rpcV2CborDenseMapsRequest);

    /**
     * <p>
     * This test case serializes JSON lists for the following cases for both input and output:
     * </p>
     * <ol>
     * <li>Normal lists.</li>
     * <li>Normal sets.</li>
     * <li>Lists of lists.</li>
     * <li>Lists of structures.</li>
     * </ol>
     * 
     * @param rpcV2CborListsRequest
     * @return Result of the RpcV2CborLists operation returned by the service.
     * @throws ValidationException
     *         A standard error for input validation failures. This should be thrown by services when a member of the
     *         input structure falls outside of the modeled or documented constraints.
     * @sample RpcV2Protocol.RpcV2CborLists
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/RpcV2CborLists" target="_top">AWS
     *      API Documentation</a>
     */
    RpcV2CborListsResult rpcV2CborLists(RpcV2CborListsRequest rpcV2CborListsRequest);

    /**
     * @param simpleScalarPropertiesRequest
     * @return Result of the SimpleScalarProperties operation returned by the service.
     * @sample RpcV2Protocol.SimpleScalarProperties
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/rpcv2protocol-2020-07-14/SimpleScalarProperties"
     *      target="_top">AWS API Documentation</a>
     */
    SimpleScalarPropertiesResult simpleScalarProperties(SimpleScalarPropertiesRequest simpleScalarPropertiesRequest);

    /**
     * Shuts down this client object, releasing any resources that might be held open. This is an optional method, and
     * callers are not expected to call it, but can if they want to explicitly release any open resources. Once a client
     * has been shutdown, it should not be used to make any more requests.
     */
    void shutdown();

    /**
     * Returns additional metadata for a previously executed successful request, typically used for debugging issues
     * where a service isn't acting as expected. This data isn't considered part of the result data returned by an
     * operation, so it's available through this separate, diagnostic interface.
     * <p>
     * Response metadata is only cached for a limited period of time, so if you need to access this extra diagnostic
     * information for an executed request, you should use this method to retrieve it as soon as possible after
     * executing a request.
     *
     * @param request
     *        The originally executed request.
     *
     * @return The response metadata for the specified request, or null if none is available.
     */
    ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request);

}

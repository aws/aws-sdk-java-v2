package software.amazon.awssdk.services.query;

import java.nio.file.Path;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.query.model.BearerAuthOperationRequest;
import software.amazon.awssdk.services.query.model.BearerAuthOperationResponse;
import software.amazon.awssdk.services.query.model.GetOperationWithChecksumRequest;
import software.amazon.awssdk.services.query.model.GetOperationWithChecksumResponse;
import software.amazon.awssdk.services.query.model.InvalidInputException;
import software.amazon.awssdk.services.query.model.OperationWithChecksumRequiredRequest;
import software.amazon.awssdk.services.query.model.OperationWithChecksumRequiredResponse;
import software.amazon.awssdk.services.query.model.OperationWithContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithCustomMemberRequest;
import software.amazon.awssdk.services.query.model.OperationWithCustomMemberResponse;
import software.amazon.awssdk.services.query.model.OperationWithCustomizedOperationContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithCustomizedOperationContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithMapOperationContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithMapOperationContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithNoneAuthTypeRequest;
import software.amazon.awssdk.services.query.model.OperationWithNoneAuthTypeResponse;
import software.amazon.awssdk.services.query.model.OperationWithOperationContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithOperationContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithRequestCompressionRequest;
import software.amazon.awssdk.services.query.model.OperationWithRequestCompressionResponse;
import software.amazon.awssdk.services.query.model.OperationWithStaticContextParamsRequest;
import software.amazon.awssdk.services.query.model.OperationWithStaticContextParamsResponse;
import software.amazon.awssdk.services.query.model.PutOperationWithChecksumRequest;
import software.amazon.awssdk.services.query.model.PutOperationWithChecksumResponse;
import software.amazon.awssdk.services.query.model.QueryException;
import software.amazon.awssdk.services.query.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.query.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.query.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.query.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.query.presignedurl.PresignedUrlManager;

/**
 * Service client for accessing Query Service. This can be created using the static {@link #builder()} method.
 *
 * A service that is implemented using the query protocol
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface QueryClient extends AwsClient {
    String SERVICE_NAME = "query-service";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "query-service";

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param aPostOperationRequest
     * @return Result of the APostOperation operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    default APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                      AwsServiceException, SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link APostOperationRequest.Builder} avoiding the need to
     * create one manually via {@link APostOperationRequest#builder()}
     * </p>
     *
     * @param aPostOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.APostOperationRequest.Builder} to create a request.
     * @return Result of the APostOperation operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    default APostOperationResponse aPostOperation(Consumer<APostOperationRequest.Builder> aPostOperationRequest)
        throws InvalidInputException, AwsServiceException, SdkClientException, QueryException {
        return aPostOperation(APostOperationRequest.builder().applyMutation(aPostOperationRequest).build());
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     * @return Result of the APostOperationWithOutput operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperationWithOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default APostOperationWithOutputResponse aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, AwsServiceException,
                                                                                SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link APostOperationWithOutputRequest.Builder} avoiding
     * the need to create one manually via {@link APostOperationWithOutputRequest#builder()}
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.APostOperationWithOutputRequest.Builder} to create a
     *        request.
     * @return Result of the APostOperationWithOutput operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperationWithOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default APostOperationWithOutputResponse aPostOperationWithOutput(
        Consumer<APostOperationWithOutputRequest.Builder> aPostOperationWithOutputRequest) throws InvalidInputException,
                                                                                                  AwsServiceException, SdkClientException, QueryException {
        return aPostOperationWithOutput(APostOperationWithOutputRequest.builder().applyMutation(aPostOperationWithOutputRequest)
                                                                       .build());
    }

    /**
     * Invokes the BearerAuthOperation operation.
     *
     * @param bearerAuthOperationRequest
     * @return Result of the BearerAuthOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default BearerAuthOperationResponse bearerAuthOperation(BearerAuthOperationRequest bearerAuthOperationRequest)
        throws AwsServiceException, SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the BearerAuthOperation operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link BearerAuthOperationRequest.Builder} avoiding the
     * need to create one manually via {@link BearerAuthOperationRequest#builder()}
     * </p>
     *
     * @param bearerAuthOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.BearerAuthOperationRequest.Builder} to create a
     *        request.
     * @return Result of the BearerAuthOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default BearerAuthOperationResponse bearerAuthOperation(
        Consumer<BearerAuthOperationRequest.Builder> bearerAuthOperationRequest) throws AwsServiceException,
                                                                                        SdkClientException, QueryException {
        return bearerAuthOperation(BearerAuthOperationRequest.builder().applyMutation(bearerAuthOperationRequest).build());
    }

    /**
     * Invokes the GetOperationWithChecksum operation.
     *
     * @param getOperationWithChecksumRequest
     * @return Result of the GetOperationWithChecksum operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/GetOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default GetOperationWithChecksumResponse getOperationWithChecksum(
        GetOperationWithChecksumRequest getOperationWithChecksumRequest) throws AwsServiceException, SdkClientException,
                                                                                QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the GetOperationWithChecksum operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetOperationWithChecksumRequest.Builder} avoiding
     * the need to create one manually via {@link GetOperationWithChecksumRequest#builder()}
     * </p>
     *
     * @param getOperationWithChecksumRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.GetOperationWithChecksumRequest.Builder} to create a
     *        request.
     * @return Result of the GetOperationWithChecksum operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/GetOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default GetOperationWithChecksumResponse getOperationWithChecksum(
        Consumer<GetOperationWithChecksumRequest.Builder> getOperationWithChecksumRequest) throws AwsServiceException,
                                                                                                  SdkClientException, QueryException {
        return getOperationWithChecksum(GetOperationWithChecksumRequest.builder().applyMutation(getOperationWithChecksumRequest)
                                                                       .build());
    }

    /**
     * Invokes the OperationWithChecksumRequired operation.
     *
     * @param operationWithChecksumRequiredRequest
     * @return Result of the OperationWithChecksumRequired operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithChecksumRequired
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithChecksumRequiredResponse operationWithChecksumRequired(
        OperationWithChecksumRequiredRequest operationWithChecksumRequiredRequest) throws AwsServiceException,
                                                                                          SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithChecksumRequired operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OperationWithChecksumRequiredRequest.Builder}
     * avoiding the need to create one manually via {@link OperationWithChecksumRequiredRequest#builder()}
     * </p>
     *
     * @param operationWithChecksumRequiredRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.OperationWithChecksumRequiredRequest.Builder} to create
     *        a request.
     * @return Result of the OperationWithChecksumRequired operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithChecksumRequired
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithChecksumRequiredResponse operationWithChecksumRequired(
        Consumer<OperationWithChecksumRequiredRequest.Builder> operationWithChecksumRequiredRequest)
        throws AwsServiceException, SdkClientException, QueryException {
        return operationWithChecksumRequired(OperationWithChecksumRequiredRequest.builder()
                                                                                 .applyMutation(operationWithChecksumRequiredRequest).build());
    }

    /**
     * Invokes the OperationWithContextParam operation.
     *
     * @param operationWithContextParamRequest
     * @return Result of the OperationWithContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithContextParam
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithContextParamResponse operationWithContextParam(
        OperationWithContextParamRequest operationWithContextParamRequest) throws AwsServiceException, SdkClientException,
                                                                                  QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithContextParam operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OperationWithContextParamRequest.Builder} avoiding
     * the need to create one manually via {@link OperationWithContextParamRequest#builder()}
     * </p>
     *
     * @param operationWithContextParamRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.OperationWithContextParamRequest.Builder} to create a
     *        request.
     * @return Result of the OperationWithContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithContextParam
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithContextParamResponse operationWithContextParam(
        Consumer<OperationWithContextParamRequest.Builder> operationWithContextParamRequest) throws AwsServiceException,
                                                                                                    SdkClientException, QueryException {
        return operationWithContextParam(OperationWithContextParamRequest.builder()
                                                                         .applyMutation(operationWithContextParamRequest).build());
    }

    /**
     * Invokes the OperationWithCustomMember operation.
     *
     * @param operationWithCustomMemberRequest
     * @return Result of the OperationWithCustomMember operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithCustomMember
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithCustomMember"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithCustomMemberResponse operationWithCustomMember(
        OperationWithCustomMemberRequest operationWithCustomMemberRequest) throws AwsServiceException, SdkClientException,
                                                                                  QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithCustomMember operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OperationWithCustomMemberRequest.Builder} avoiding
     * the need to create one manually via {@link OperationWithCustomMemberRequest#builder()}
     * </p>
     *
     * @param operationWithCustomMemberRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.OperationWithCustomMemberRequest.Builder} to create a
     *        request.
     * @return Result of the OperationWithCustomMember operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithCustomMember
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithCustomMember"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithCustomMemberResponse operationWithCustomMember(
        Consumer<OperationWithCustomMemberRequest.Builder> operationWithCustomMemberRequest) throws AwsServiceException,
                                                                                                    SdkClientException, QueryException {
        return operationWithCustomMember(OperationWithCustomMemberRequest.builder()
                                                                         .applyMutation(operationWithCustomMemberRequest).build());
    }

    /**
     * Invokes the OperationWithCustomizedOperationContextParam operation.
     *
     * @param operationWithCustomizedOperationContextParamRequest
     * @return Result of the OperationWithCustomizedOperationContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithCustomizedOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithCustomizedOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithCustomizedOperationContextParamResponse operationWithCustomizedOperationContextParam(
        OperationWithCustomizedOperationContextParamRequest operationWithCustomizedOperationContextParamRequest)
        throws AwsServiceException, SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithCustomizedOperationContextParam operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the
     * {@link OperationWithCustomizedOperationContextParamRequest.Builder} avoiding the need to create one manually via
     * {@link OperationWithCustomizedOperationContextParamRequest#builder()}
     * </p>
     *
     * @param operationWithCustomizedOperationContextParamRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.OperationWithCustomizedOperationContextParamRequest.Builder}
     *        to create a request.
     * @return Result of the OperationWithCustomizedOperationContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithCustomizedOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithCustomizedOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithCustomizedOperationContextParamResponse operationWithCustomizedOperationContextParam(
        Consumer<OperationWithCustomizedOperationContextParamRequest.Builder> operationWithCustomizedOperationContextParamRequest)
        throws AwsServiceException, SdkClientException, QueryException {
        return operationWithCustomizedOperationContextParam(OperationWithCustomizedOperationContextParamRequest.builder()
                                                                                                               .applyMutation(operationWithCustomizedOperationContextParamRequest).build());
    }

    /**
     * Invokes the OperationWithMapOperationContextParam operation.
     *
     * @param operationWithMapOperationContextParamRequest
     * @return Result of the OperationWithMapOperationContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithMapOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithMapOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithMapOperationContextParamResponse operationWithMapOperationContextParam(
        OperationWithMapOperationContextParamRequest operationWithMapOperationContextParamRequest)
        throws AwsServiceException, SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithMapOperationContextParam operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the
     * {@link OperationWithMapOperationContextParamRequest.Builder} avoiding the need to create one manually via
     * {@link OperationWithMapOperationContextParamRequest#builder()}
     * </p>
     *
     * @param operationWithMapOperationContextParamRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.OperationWithMapOperationContextParamRequest.Builder}
     *        to create a request.
     * @return Result of the OperationWithMapOperationContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithMapOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithMapOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithMapOperationContextParamResponse operationWithMapOperationContextParam(
        Consumer<OperationWithMapOperationContextParamRequest.Builder> operationWithMapOperationContextParamRequest)
        throws AwsServiceException, SdkClientException, QueryException {
        return operationWithMapOperationContextParam(OperationWithMapOperationContextParamRequest.builder()
                                                                                                 .applyMutation(operationWithMapOperationContextParamRequest).build());
    }

    /**
     * Invokes the OperationWithNoneAuthType operation.
     *
     * @param operationWithNoneAuthTypeRequest
     * @return Result of the OperationWithNoneAuthType operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithNoneAuthType
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithNoneAuthType"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithNoneAuthTypeResponse operationWithNoneAuthType(
        OperationWithNoneAuthTypeRequest operationWithNoneAuthTypeRequest) throws AwsServiceException, SdkClientException,
                                                                                  QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithNoneAuthType operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OperationWithNoneAuthTypeRequest.Builder} avoiding
     * the need to create one manually via {@link OperationWithNoneAuthTypeRequest#builder()}
     * </p>
     *
     * @param operationWithNoneAuthTypeRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.OperationWithNoneAuthTypeRequest.Builder} to create a
     *        request.
     * @return Result of the OperationWithNoneAuthType operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithNoneAuthType
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithNoneAuthType"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithNoneAuthTypeResponse operationWithNoneAuthType(
        Consumer<OperationWithNoneAuthTypeRequest.Builder> operationWithNoneAuthTypeRequest) throws AwsServiceException,
                                                                                                    SdkClientException, QueryException {
        return operationWithNoneAuthType(OperationWithNoneAuthTypeRequest.builder()
                                                                         .applyMutation(operationWithNoneAuthTypeRequest).build());
    }

    /**
     * Invokes the OperationWithOperationContextParam operation.
     *
     * @param operationWithOperationContextParamRequest
     * @return Result of the OperationWithOperationContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithOperationContextParamResponse operationWithOperationContextParam(
        OperationWithOperationContextParamRequest operationWithOperationContextParamRequest) throws AwsServiceException,
                                                                                                    SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithOperationContextParam operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OperationWithOperationContextParamRequest.Builder}
     * avoiding the need to create one manually via {@link OperationWithOperationContextParamRequest#builder()}
     * </p>
     *
     * @param operationWithOperationContextParamRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.OperationWithOperationContextParamRequest.Builder} to
     *        create a request.
     * @return Result of the OperationWithOperationContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithOperationContextParamResponse operationWithOperationContextParam(
        Consumer<OperationWithOperationContextParamRequest.Builder> operationWithOperationContextParamRequest)
        throws AwsServiceException, SdkClientException, QueryException {
        return operationWithOperationContextParam(OperationWithOperationContextParamRequest.builder()
                                                                                           .applyMutation(operationWithOperationContextParamRequest).build());
    }

    /**
     * Invokes the OperationWithRequestCompression operation.
     *
     * @param operationWithRequestCompressionRequest
     * @return Result of the OperationWithRequestCompression operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithRequestCompression"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithRequestCompressionResponse operationWithRequestCompression(
        OperationWithRequestCompressionRequest operationWithRequestCompressionRequest) throws AwsServiceException,
                                                                                              SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithRequestCompression operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OperationWithRequestCompressionRequest.Builder}
     * avoiding the need to create one manually via {@link OperationWithRequestCompressionRequest#builder()}
     * </p>
     *
     * @param operationWithRequestCompressionRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.OperationWithRequestCompressionRequest.Builder} to
     *        create a request.
     * @return Result of the OperationWithRequestCompression operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithRequestCompression"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithRequestCompressionResponse operationWithRequestCompression(
        Consumer<OperationWithRequestCompressionRequest.Builder> operationWithRequestCompressionRequest)
        throws AwsServiceException, SdkClientException, QueryException {
        return operationWithRequestCompression(OperationWithRequestCompressionRequest.builder()
                                                                                     .applyMutation(operationWithRequestCompressionRequest).build());
    }

    /**
     * Invokes the OperationWithStaticContextParams operation.
     *
     * @param operationWithStaticContextParamsRequest
     * @return Result of the OperationWithStaticContextParams operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithStaticContextParams
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithStaticContextParams"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithStaticContextParamsResponse operationWithStaticContextParams(
        OperationWithStaticContextParamsRequest operationWithStaticContextParamsRequest) throws AwsServiceException,
                                                                                                SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithStaticContextParams operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OperationWithStaticContextParamsRequest.Builder}
     * avoiding the need to create one manually via {@link OperationWithStaticContextParamsRequest#builder()}
     * </p>
     *
     * @param operationWithStaticContextParamsRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.OperationWithStaticContextParamsRequest.Builder} to
     *        create a request.
     * @return Result of the OperationWithStaticContextParams operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithStaticContextParams
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithStaticContextParams"
     *      target="_top">AWS API Documentation</a>
     */
    default OperationWithStaticContextParamsResponse operationWithStaticContextParams(
        Consumer<OperationWithStaticContextParamsRequest.Builder> operationWithStaticContextParamsRequest)
        throws AwsServiceException, SdkClientException, QueryException {
        return operationWithStaticContextParams(OperationWithStaticContextParamsRequest.builder()
                                                                                       .applyMutation(operationWithStaticContextParamsRequest).build());
    }

    /**
     * Invokes the PutOperationWithChecksum operation.
     *
     * @param putOperationWithChecksumRequest
     * @param requestBody
     *        The content to send to the service. A {@link RequestBody} can be created using one of several factory
     *        methods for various sources of data. For example, to create a request body from a file you can do the
     *        following.
     *
     *        <pre>
     * {@code RequestBody.fromFile(new File("myfile.txt"))}
     * </pre>
     *
     *        See documentation in {@link RequestBody} for additional details and which sources of data are supported.
     *        The service documentation for the request content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        PutOperationWithChecksumResponse and an InputStream to the response content are provided as parameters to
     *        the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> ReturnT putOperationWithChecksum(PutOperationWithChecksumRequest putOperationWithChecksumRequest,
                                                       RequestBody requestBody, ResponseTransformer<PutOperationWithChecksumResponse, ReturnT> responseTransformer)
        throws AwsServiceException, SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PutOperationWithChecksum operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PutOperationWithChecksumRequest.Builder} avoiding
     * the need to create one manually via {@link PutOperationWithChecksumRequest#builder()}
     * </p>
     *
     * @param putOperationWithChecksumRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.PutOperationWithChecksumRequest.Builder} to create a
     *        request.
     * @param requestBody
     *        The content to send to the service. A {@link RequestBody} can be created using one of several factory
     *        methods for various sources of data. For example, to create a request body from a file you can do the
     *        following.
     *
     *        <pre>
     * {@code RequestBody.fromFile(new File("myfile.txt"))}
     * </pre>
     *
     *        See documentation in {@link RequestBody} for additional details and which sources of data are supported.
     *        The service documentation for the request content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        PutOperationWithChecksumResponse and an InputStream to the response content are provided as parameters to
     *        the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> ReturnT putOperationWithChecksum(
        Consumer<PutOperationWithChecksumRequest.Builder> putOperationWithChecksumRequest, RequestBody requestBody,
        ResponseTransformer<PutOperationWithChecksumResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                   SdkClientException, QueryException {
        return putOperationWithChecksum(PutOperationWithChecksumRequest.builder().applyMutation(putOperationWithChecksumRequest)
                                                                       .build(), requestBody, responseTransformer);
    }

    /**
     * Invokes the PutOperationWithChecksum operation.
     *
     * @param putOperationWithChecksumRequest
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.PutOperationWithChecksum
     * @see #putOperationWithChecksum(PutOperationWithChecksumRequest, RequestBody)
     * @see #putOperationWithChecksum(PutOperationWithChecksumRequest, ResponseTransformer)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default PutOperationWithChecksumResponse putOperationWithChecksum(
        PutOperationWithChecksumRequest putOperationWithChecksumRequest, Path sourcePath, Path destinationPath)
        throws AwsServiceException, SdkClientException, QueryException {
        return putOperationWithChecksum(putOperationWithChecksumRequest, RequestBody.fromFile(sourcePath),
                                        ResponseTransformer.toFile(destinationPath));
    }

    /**
     * Invokes the PutOperationWithChecksum operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PutOperationWithChecksumRequest.Builder} avoiding
     * the need to create one manually via {@link PutOperationWithChecksumRequest#builder()}
     * </p>
     *
     * @param putOperationWithChecksumRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.PutOperationWithChecksumRequest.Builder} to create a
     *        request.
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.PutOperationWithChecksum
     * @see #putOperationWithChecksum(PutOperationWithChecksumRequest, RequestBody)
     * @see #putOperationWithChecksum(PutOperationWithChecksumRequest, ResponseTransformer)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default PutOperationWithChecksumResponse putOperationWithChecksum(
        Consumer<PutOperationWithChecksumRequest.Builder> putOperationWithChecksumRequest, Path sourcePath,
        Path destinationPath) throws AwsServiceException, SdkClientException, QueryException {
        return putOperationWithChecksum(PutOperationWithChecksumRequest.builder().applyMutation(putOperationWithChecksumRequest)
                                                                       .build(), sourcePath, destinationPath);
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param requestBody
     *        The content to send to the service. A {@link RequestBody} can be created using one of several factory
     *        methods for various sources of data. For example, to create a request body from a file you can do the
     *        following.
     *
     *        <pre>
     * {@code RequestBody.fromFile(new File("myfile.txt"))}
     * </pre>
     *
     *        See documentation in {@link RequestBody} for additional details and which sources of data are supported.
     *        The service documentation for the request content is as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingInputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOperationResponse streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, RequestBody requestBody) throws AwsServiceException,
                                                                                                       SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming input<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingInputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.StreamingInputOperationRequest.Builder} to create a
     *        request.
     * @param requestBody
     *        The content to send to the service. A {@link RequestBody} can be created using one of several factory
     *        methods for various sources of data. For example, to create a request body from a file you can do the
     *        following.
     *
     *        <pre>
     * {@code RequestBody.fromFile(new File("myfile.txt"))}
     * </pre>
     *
     *        See documentation in {@link RequestBody} for additional details and which sources of data are supported.
     *        The service documentation for the request content is as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingInputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOperationResponse streamingInputOperation(
        Consumer<StreamingInputOperationRequest.Builder> streamingInputOperationRequest, RequestBody requestBody)
        throws AwsServiceException, SdkClientException, QueryException {
        return streamingInputOperation(StreamingInputOperationRequest.builder().applyMutation(streamingInputOperationRequest)
                                                                     .build(), requestBody);
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingInputOperation
     * @see #streamingInputOperation(StreamingInputOperationRequest, RequestBody)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOperationResponse streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, Path sourcePath) throws AwsServiceException,
                                                                                               SdkClientException, QueryException {
        return streamingInputOperation(streamingInputOperationRequest, RequestBody.fromFile(sourcePath));
    }

    /**
     * Some operation with a streaming input<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingInputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.StreamingInputOperationRequest.Builder} to create a
     *        request.
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingInputOperation
     * @see #streamingInputOperation(StreamingInputOperationRequest, RequestBody)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingInputOperationResponse streamingInputOperation(
        Consumer<StreamingInputOperationRequest.Builder> streamingInputOperationRequest, Path sourcePath)
        throws AwsServiceException, SdkClientException, QueryException {
        return streamingInputOperation(StreamingInputOperationRequest.builder().applyMutation(streamingInputOperationRequest)
                                                                     .build(), sourcePath);
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingOutputOperationResponse and an InputStream to the response content are provided as parameters to
     *        the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> ReturnT streamingOutputOperation(StreamingOutputOperationRequest streamingOutputOperationRequest,
                                                       ResponseTransformer<StreamingOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                                                                  SdkClientException, QueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.StreamingOutputOperationRequest.Builder} to create a
     *        request.
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingOutputOperationResponse and an InputStream to the response content are provided as parameters to
     *        the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> ReturnT streamingOutputOperation(
        Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest,
        ResponseTransformer<StreamingOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                   SdkClientException, QueryException {
        return streamingOutputOperation(StreamingOutputOperationRequest.builder().applyMutation(streamingOutputOperationRequest)
                                                                       .build(), responseTransformer);
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingOutputOperation
     * @see #streamingOutputOperation(StreamingOutputOperationRequest, ResponseTransformer)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingOutputOperationResponse streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest, Path destinationPath) throws AwsServiceException,
                                                                                                      SdkClientException, QueryException {
        return streamingOutputOperation(streamingOutputOperationRequest, ResponseTransformer.toFile(destinationPath));
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.StreamingOutputOperationRequest.Builder} to create a
     *        request.
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingOutputOperation
     * @see #streamingOutputOperation(StreamingOutputOperationRequest, ResponseTransformer)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default StreamingOutputOperationResponse streamingOutputOperation(
        Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest, Path destinationPath)
        throws AwsServiceException, SdkClientException, QueryException {
        return streamingOutputOperation(StreamingOutputOperationRequest.builder().applyMutation(streamingOutputOperationRequest)
                                                                       .build(), destinationPath);
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @return A {@link ResponseInputStream} containing data streamed from service. Note that this is an unmanaged
     *         reference to the underlying HTTP connection so great care must be taken to ensure all data if fully read
     *         from the input stream and that it is properly closed. Failure to do so may result in sub-optimal behavior
     *         and exhausting connections in the connection pool. The unmarshalled response object can be obtained via
     *         {@link ResponseInputStream#response()}. The service documentation for the response content is as follows
     *         'This be a stream'.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingOutputOperation
     * @see #getObject(streamingOutputOperation, ResponseTransformer)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default ResponseInputStream<StreamingOutputOperationResponse> streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest) throws AwsServiceException, SdkClientException,
                                                                                QueryException {
        return streamingOutputOperation(streamingOutputOperationRequest, ResponseTransformer.toInputStream());
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.StreamingOutputOperationRequest.Builder} to create a
     *        request.
     * @return A {@link ResponseInputStream} containing data streamed from service. Note that this is an unmanaged
     *         reference to the underlying HTTP connection so great care must be taken to ensure all data if fully read
     *         from the input stream and that it is properly closed. Failure to do so may result in sub-optimal behavior
     *         and exhausting connections in the connection pool. The unmarshalled response object can be obtained via
     *         {@link ResponseInputStream#response()}. The service documentation for the response content is as follows
     *         'This be a stream'.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingOutputOperation
     * @see #getObject(streamingOutputOperation, ResponseTransformer)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default ResponseInputStream<StreamingOutputOperationResponse> streamingOutputOperation(
        Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest) throws AwsServiceException,
                                                                                                  SdkClientException, QueryException {
        return streamingOutputOperation(StreamingOutputOperationRequest.builder().applyMutation(streamingOutputOperationRequest)
                                                                       .build());
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @return A {@link ResponseBytes} that loads the data streamed from the service into memory and exposes it in
     *         convenient in-memory representations like a byte buffer or string. The unmarshalled response object can
     *         be obtained via {@link ResponseBytes#response()}. The service documentation for the response content is
     *         as follows 'This be a stream'.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingOutputOperation
     * @see #getObject(streamingOutputOperation, ResponseTransformer)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default ResponseBytes<StreamingOutputOperationResponse> streamingOutputOperationAsBytes(
        StreamingOutputOperationRequest streamingOutputOperationRequest) throws AwsServiceException, SdkClientException,
                                                                                QueryException {
        return streamingOutputOperation(streamingOutputOperationRequest, ResponseTransformer.toBytes());
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.query.model.StreamingOutputOperationRequest.Builder} to create a
     *        request.
     * @return A {@link ResponseBytes} that loads the data streamed from the service into memory and exposes it in
     *         convenient in-memory representations like a byte buffer or string. The unmarshalled response object can
     *         be obtained via {@link ResponseBytes#response()}. The service documentation for the response content is
     *         as follows 'This be a stream'.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingOutputOperation
     * @see #getObject(streamingOutputOperation, ResponseTransformer)
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default ResponseBytes<StreamingOutputOperationResponse> streamingOutputOperationAsBytes(
        Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest) throws AwsServiceException,
                                                                                                  SdkClientException, QueryException {
        return streamingOutputOperationAsBytes(StreamingOutputOperationRequest.builder()
                                                                              .applyMutation(streamingOutputOperationRequest).build());
    }

    /**
     * Creates an instance of {@link PresignedUrlManager} object with the configuration set on this client.
     */
    default PresignedUrlManager presignedUrlManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a {@link QueryClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static QueryClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link QueryClient}.
     */
    static QueryClientBuilder builder() {
        return new DefaultQueryClientBuilder();
    }

    static ServiceMetadata serviceMetadata() {
        return ServiceMetadata.of(SERVICE_METADATA_ID);
    }

    @Override
    default QueryServiceClientConfiguration serviceClientConfiguration() {
        throw new UnsupportedOperationException();
    }
}

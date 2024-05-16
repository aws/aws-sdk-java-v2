import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.utils.ToString;

/**
 * Contains classes used at runtime by the code generator classes for waiter acceptors generated from JMESPath
 * expressions.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class WaitersRuntime {

    private static final String FAILURE_MESSAGE_FORMAT_FOR_STATUS_MATCHER = "A waiter acceptor was matched to response "
                                                                            + "status %d and transitioned the waiter to failure"
                                                                            + " state";

    /**
     * The default acceptors that should be matched *last* in the list of acceptors used by the SDK client waiters.
     */
    public static final List<WaiterAcceptor<Object>> DEFAULT_ACCEPTORS = Collections.unmodifiableList(defaultAcceptors());

    private WaitersRuntime() {
    }

    private static List<WaiterAcceptor<Object>> defaultAcceptors() {
        return Collections.singletonList(retryOnUnmatchedResponseWaiter());
    }

    private static WaiterAcceptor<Object> retryOnUnmatchedResponseWaiter() {
        return WaiterAcceptor.retryOnResponseAcceptor(r -> true);
    }

    /**
     * A {@link WaiterAcceptor} implementation that checks for a specific HTTP response status, regardless of whether it's
     * reported by a response or an exception.
     */
    public static final class ResponseStatusAcceptor implements WaiterAcceptor<SdkResponse> {
        private final int statusCode;
        private final WaiterState waiterState;

        public ResponseStatusAcceptor(int statusCode, WaiterState waiterState) {
            this.statusCode = statusCode;
            this.waiterState = waiterState;
        }

        @Override
        public WaiterState waiterState() {
            return waiterState;
        }

        @Override
        public boolean matches(SdkResponse response) {
            return response.sdkHttpResponse() != null &&
                   response.sdkHttpResponse().statusCode() == statusCode;
        }

        @Override
        public boolean matches(Throwable throwable) {
            if (throwable instanceof SdkServiceException) {
                return ((SdkServiceException) throwable).statusCode() == statusCode;
            }

            return false;
        }

        @Override
        public Optional<String> message() {
            return waiterState == WaiterState.FAILURE ? Optional.of(String.format(FAILURE_MESSAGE_FORMAT_FOR_STATUS_MATCHER,
                    statusCode)) : Optional.empty();
        }
    }
}

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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * Unit tests for {@link AuthErrorInvalidationHelper}.
 */
public class AuthErrorInvalidationHelperTest {

    @ParameterizedTest
    @ValueSource(strings = {"ExpiredToken", "InvalidToken", "AuthFailure"})
    void invalidateIfAuthError_whenAuthErrorCode_triggersInvalidation(String errorCode) {
        // Arrange
        TrackingIdentityProvider provider = new TrackingIdentityProvider();
        TestIdentity identity = new TestIdentity();
        RequestExecutionContext context = contextWithProvider(provider, identity);
        Throwable exception = serviceExceptionWithErrorCode(errorCode);

        // Act
        AuthErrorInvalidationHelper.invalidateIfAuthError(exception, context);

        // Assert
        assertThat(provider.invalidateCalled()).isTrue();
        assertThat(provider.lastInvalidatedIdentity()).isSameAs(identity);
    }

    @Test
    void invalidateIfAuthError_whenAccessDenied_doesNotTriggerInvalidation() {
        // Arrange
        TrackingIdentityProvider provider = new TrackingIdentityProvider();
        TestIdentity identity = new TestIdentity();
        RequestExecutionContext context = contextWithProvider(provider, identity);
        Throwable exception = serviceExceptionWithErrorCode("AccessDenied");

        // Act
        AuthErrorInvalidationHelper.invalidateIfAuthError(exception, context);

        // Assert
        assertThat(provider.invalidateCalled()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ThrottlingException", "InternalServerError", "ValidationException", "ResourceNotFoundException"})
    void invalidateIfAuthError_whenUnknownErrorCode_doesNotTriggerInvalidation(String errorCode) {
        // Arrange
        TrackingIdentityProvider provider = new TrackingIdentityProvider();
        TestIdentity identity = new TestIdentity();
        RequestExecutionContext context = contextWithProvider(provider, identity);
        Throwable exception = serviceExceptionWithErrorCode(errorCode);

        // Act
        AuthErrorInvalidationHelper.invalidateIfAuthError(exception, context);

        // Assert
        assertThat(provider.invalidateCalled()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("nonServiceExceptions")
    void invalidateIfAuthError_whenNonSdkServiceException_doesNotTriggerInvalidation(Throwable exception) {
        // Arrange
        TrackingIdentityProvider provider = new TrackingIdentityProvider();
        TestIdentity identity = new TestIdentity();
        RequestExecutionContext context = contextWithProvider(provider, identity);

        // Act
        AuthErrorInvalidationHelper.invalidateIfAuthError(exception, context);

        // Assert
        assertThat(provider.invalidateCalled()).isFalse();
    }

    static Stream<Throwable> nonServiceExceptions() {
        return Stream.of(
            new RuntimeException("something went wrong"),
            SdkClientException.create("client error"),
            new IOException("io error")
        );
    }

    @Test
    void invalidateIfAuthError_whenSelectedAuthSchemeIsNull_doesNotThrow() {
        // Arrange — context with no SELECTED_AUTH_SCHEME attribute
        RequestExecutionContext context = contextWithNoAuthScheme();
        Throwable exception = serviceExceptionWithErrorCode("ExpiredToken");

        // Act & Assert — no NPE, no invalidation
        assertThatNoException().isThrownBy(() ->
            AuthErrorInvalidationHelper.invalidateIfAuthError(exception, context)
        );
    }

    @Test
    void invalidateIfAuthError_whenIdentityProviderIsNull_doesNotThrow() {
        // Arrange — SelectedAuthScheme created with 3-arg constructor (null identityProvider)
        TestIdentity identity = new TestIdentity();
        SelectedAuthScheme<TestIdentity> selectedAuthScheme = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(identity),
            mockSigner(),
            AuthSchemeOption.builder().schemeId("test").build()
        );

        RequestExecutionContext context = contextWithSelectedAuthScheme(selectedAuthScheme);
        Throwable exception = serviceExceptionWithErrorCode("ExpiredToken");

        // Act & Assert — no NPE, no invalidation
        assertThatNoException().isThrownBy(() ->
            AuthErrorInvalidationHelper.invalidateIfAuthError(exception, context)
        );
    }

    @Test
    void invalidateIfAuthError_whenInvalidateThrowsException_doesNotPropagate() {
        // Arrange — provider that throws on invalidate()
        ThrowingIdentityProvider provider = new ThrowingIdentityProvider();
        TestIdentity identity = new TestIdentity();
        SelectedAuthScheme<TestIdentity> selectedAuthScheme = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(identity),
            mockSigner(),
            AuthSchemeOption.builder().schemeId("test").build(),
            provider
        );

        RequestExecutionContext context = contextWithSelectedAuthScheme(selectedAuthScheme);
        Throwable exception = serviceExceptionWithErrorCode("ExpiredToken");

        // Act & Assert — exception is caught internally, method returns normally
        assertThatNoException().isThrownBy(() ->
            AuthErrorInvalidationHelper.invalidateIfAuthError(exception, context)
        );
    }

    // --- Helper methods ---

    private RequestExecutionContext contextWithProvider(TrackingIdentityProvider provider, TestIdentity identity) {
        SelectedAuthScheme<TestIdentity> selectedAuthScheme = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(identity),
            mockSigner(),
            AuthSchemeOption.builder().schemeId("test").build(),
            provider
        );
        return contextWithSelectedAuthScheme(selectedAuthScheme);
    }

    private RequestExecutionContext contextWithSelectedAuthScheme(SelectedAuthScheme<?> selectedAuthScheme) {
        ExecutionAttributes executionAttributes = ExecutionAttributes.builder()
            .put(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme)
            .build();

        ExecutionContext executionContext = ExecutionContext.builder()
            .executionAttributes(executionAttributes)
            .build();

        return RequestExecutionContext.builder()
            .executionContext(executionContext)
            .originalRequest(mock(SdkRequest.class))
            .build();
    }

    private RequestExecutionContext contextWithNoAuthScheme() {
        ExecutionAttributes executionAttributes = ExecutionAttributes.builder().build();

        ExecutionContext executionContext = ExecutionContext.builder()
            .executionAttributes(executionAttributes)
            .build();

        return RequestExecutionContext.builder()
            .executionContext(executionContext)
            .originalRequest(mock(SdkRequest.class))
            .build();
    }

    /**
     * Creates an SdkServiceException subclass that overrides {@code isAuthenticationError()} to
     * return true for the known auth error codes.
     */
    private Throwable serviceExceptionWithErrorCode(String errorCode) {
        return new TestAwsServiceException(errorCode);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Identity> HttpSigner<T> mockSigner() {
        return (HttpSigner<T>) mock(HttpSigner.class);
    }

    // --- Test doubles ---

    /**
     * A simple identity implementation for testing.
     */
    private static class TestIdentity implements Identity {
    }

    /**
     * An identity provider that tracks whether invalidate() was called.
     */
    private static class TrackingIdentityProvider implements IdentityProvider<TestIdentity> {
        private final AtomicBoolean invalidateCalled = new AtomicBoolean(false);
        private final AtomicReference<TestIdentity> lastInvalidatedIdentity = new AtomicReference<>();

        @Override
        public Class<TestIdentity> identityType() {
            return TestIdentity.class;
        }

        @Override
        public CompletableFuture<TestIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(new TestIdentity());
        }

        @Override
        public CompletableFuture<Void> invalidate(TestIdentity identity) {
            invalidateCalled.set(true);
            lastInvalidatedIdentity.set(identity);
            return CompletableFuture.completedFuture(null);
        }

        boolean invalidateCalled() {
            return invalidateCalled.get();
        }

        TestIdentity lastInvalidatedIdentity() {
            return lastInvalidatedIdentity.get();
        }
    }

    /**
     * An identity provider that throws on invalidate() — used to test exception isolation.
     */
    private static class ThrowingIdentityProvider implements IdentityProvider<TestIdentity> {
        @Override
        public Class<TestIdentity> identityType() {
            return TestIdentity.class;
        }

        @Override
        public CompletableFuture<TestIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(new TestIdentity());
        }

        @Override
        public CompletableFuture<Void> invalidate(TestIdentity identity) {
            throw new RuntimeException("Simulated invalidation failure");
        }
    }

    /**
     * Simulates an AwsServiceException that reports authentication errors via the
     * {@link SdkServiceException#isAuthenticationError()} virtual method.
     */
    private static class TestAwsServiceException extends SdkServiceException {
        private static final Set<String> AUTH_ERROR_CODES = new HashSet<>(Arrays.asList(
            "ExpiredToken", "InvalidToken", "AuthFailure"
        ));

        private final String errorCode;

        TestAwsServiceException(String errorCode) {
            super(SdkServiceException.builder().message("test exception").statusCode(401));
            this.errorCode = errorCode;
        }

        @Override
        public boolean isAuthenticationError() {
            return AUTH_ERROR_CODES.contains(errorCode);
        }
    }
}

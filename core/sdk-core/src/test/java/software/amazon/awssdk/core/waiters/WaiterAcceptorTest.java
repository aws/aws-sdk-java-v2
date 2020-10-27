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

package software.amazon.awssdk.core.waiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Predicate;
import org.testng.annotations.Test;


public class WaiterAcceptorTest {
    private static final String STRING_TO_MATCH = "foobar";
    private static final Predicate<String> STRING_PREDICATE = s -> s.equals(STRING_TO_MATCH);
    private static final Predicate<Throwable> EXCEPTION_PREDICATE = s -> s.getMessage().equals(STRING_TO_MATCH);

    @Test
    public void successOnResponseAcceptor() {
        WaiterAcceptor<String> objectWaiterAcceptor = WaiterAcceptor
            .successOnResponseAcceptor(STRING_PREDICATE);

        assertThat(objectWaiterAcceptor.matches(STRING_TO_MATCH)).isTrue();
        assertThat(objectWaiterAcceptor.matches("blah")).isFalse();
        assertThat(objectWaiterAcceptor.waiterState()).isEqualTo(WaiterState.SUCCESS);
        assertThat(objectWaiterAcceptor.message()).isEmpty();
    }

    @Test
    public void errorOnResponseAcceptor() {
        WaiterAcceptor<String> objectWaiterAcceptor = WaiterAcceptor
            .errorOnResponseAcceptor(STRING_PREDICATE);

        assertThat(objectWaiterAcceptor.matches(STRING_TO_MATCH)).isTrue();
        assertThat(objectWaiterAcceptor.matches("blah")).isFalse();
        assertThat(objectWaiterAcceptor.waiterState()).isEqualTo(WaiterState.FAILURE);
        assertThat(objectWaiterAcceptor.message()).isEmpty();
    }

    @Test
    public void errorOnResponseAcceptorWithMsg() {
        WaiterAcceptor<String> objectWaiterAcceptor = WaiterAcceptor
            .errorOnResponseAcceptor(STRING_PREDICATE, "wrong response");

        assertThat(objectWaiterAcceptor.matches(STRING_TO_MATCH)).isTrue();
        assertThat(objectWaiterAcceptor.matches("blah")).isFalse();
        assertThat(objectWaiterAcceptor.waiterState()).isEqualTo(WaiterState.FAILURE);
        assertThat(objectWaiterAcceptor.message()).contains("wrong response");
    }

    @Test
    public void errorOnResponseAcceptor_nullMsg_shouldThrowException() {
        assertThatThrownBy(() -> WaiterAcceptor
            .errorOnResponseAcceptor(STRING_PREDICATE, null)).hasMessageContaining("message must not be null");
    }

    @Test
    public void errorOnResponseAcceptor_nullPredicate_shouldThrowException() {
        assertThatThrownBy(() -> WaiterAcceptor
            .errorOnResponseAcceptor(null)).hasMessageContaining("responsePredicate must not be null");
    }

    @Test
    public void successOnExceptionAcceptor() {
        WaiterAcceptor<String> objectWaiterAcceptor = WaiterAcceptor
            .successOnExceptionAcceptor(EXCEPTION_PREDICATE);

        assertThat(objectWaiterAcceptor.matches(new RuntimeException(STRING_TO_MATCH))).isTrue();
        assertThat(objectWaiterAcceptor.matches(new RuntimeException("blah"))).isFalse();
        assertThat(objectWaiterAcceptor.waiterState()).isEqualTo(WaiterState.SUCCESS);
        assertThat(objectWaiterAcceptor.message()).isEmpty();
    }

    @Test
    public void errorOnExceptionAcceptor() {
        WaiterAcceptor<String> objectWaiterAcceptor = WaiterAcceptor
            .errorOnExceptionAcceptor(EXCEPTION_PREDICATE);

        assertThat(objectWaiterAcceptor.matches(new RuntimeException(STRING_TO_MATCH))).isTrue();
        assertThat(objectWaiterAcceptor.matches(new RuntimeException("blah"))).isFalse();
        assertThat(objectWaiterAcceptor.waiterState()).isEqualTo(WaiterState.FAILURE);
        assertThat(objectWaiterAcceptor.message()).isEmpty();
    }

    @Test
    public void retryOnExceptionAcceptor() {
        WaiterAcceptor<String> objectWaiterAcceptor = WaiterAcceptor
            .retryOnExceptionAcceptor(EXCEPTION_PREDICATE);

        assertThat(objectWaiterAcceptor.matches(new RuntimeException(STRING_TO_MATCH))).isTrue();
        assertThat(objectWaiterAcceptor.matches(new RuntimeException("blah"))).isFalse();
        assertThat(objectWaiterAcceptor.waiterState()).isEqualTo(WaiterState.RETRY);
        assertThat(objectWaiterAcceptor.message()).isEmpty();
    }
}

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

package software.amazon.awssdk.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.http.ConnectionPoolShutdownException.Reason;

class ConnectionPoolShutdownExceptionTest {

    @ParameterizedTest
    @EnumSource(Reason.class)
    void create_anyReason_keepsReasonCauseAndExactMessage(Reason reason) {
        IllegalStateException cause = new IllegalStateException("Connection pool shut down");

        ConnectionPoolShutdownException e = ConnectionPoolShutdownException.create(reason, cause);

        assertThat(e.reason()).isEqualTo(reason);
        assertThat(e.getCause()).isSameAs(cause);
        assertThat(e.getMessage()).isEqualTo("Connection pool shut down");
        assertThat(e).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_nullCause_isAllowed() {
        ConnectionPoolShutdownException e = ConnectionPoolShutdownException.create(Reason.CLIENT_CLOSED, null);

        assertThat(e.getCause()).isNull();
        assertThat(e.getMessage()).isEqualTo("Connection pool shut down");
    }

    @Test
    void create_nullReason_throwsNullPointerException() {
        assertThatThrownBy(() -> ConnectionPoolShutdownException.create(null, new IllegalStateException()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("reason");
    }

    @Test
    void serialization_roundTrip_preservesReasonAndMessage() throws IOException, ClassNotFoundException {
        ConnectionPoolShutdownException original =
            ConnectionPoolShutdownException.create(Reason.RECOVERY_BACKOFF,
                                                   new IllegalStateException("Connection pool shut down"));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        Object copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = in.readObject();
        }

        assertThat(copy).isInstanceOf(ConnectionPoolShutdownException.class);
        ConnectionPoolShutdownException deserialized = (ConnectionPoolShutdownException) copy;
        assertThat(deserialized.reason()).isEqualTo(Reason.RECOVERY_BACKOFF);
        assertThat(deserialized.getMessage()).isEqualTo("Connection pool shut down");
    }

    @Test
    void reasonValues_areExactlyTheDocumentedConstants() {
        assertThat(Reason.values()).containsExactly(Reason.CLIENT_CLOSED,
                                                    Reason.SHUT_DOWN_AFTER_ERROR,
                                                    Reason.RECOVERY_BACKOFF);
    }
}

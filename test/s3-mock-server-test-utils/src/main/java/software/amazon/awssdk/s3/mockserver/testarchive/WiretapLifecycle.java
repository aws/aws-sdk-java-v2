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

package software.amazon.awssdk.s3.mockserver.testarchive;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.s3.mockserver.invariants.ObservationSink;
import software.amazon.awssdk.s3.mockserver.server.S3Wiretap;

public class WiretapLifecycle implements BeforeEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(WiretapLifecycle.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        S3TestBase test = (S3TestBase) context.getRequiredTestInstance();
        S3Wiretap wiretap = containerOf(context)
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(Thread.currentThread().getName(),
                        key -> open(test.mode()), Holder.class)
                .wiretap();
        test.attachWiretap(wiretap);
        test.attachSink(ObservationSink.open(context.getRequiredTestClass().getSimpleName() + "#"
                + context.getRequiredTestMethod().getName() + " " + context.getDisplayName()));
    }

    private static ExtensionContext containerOf(ExtensionContext context) {
        ExtensionContext current = context;
        while (current.getTestMethod().isPresent() && current.getParent().isPresent()) {
            current = current.getParent().get();
        }
        return current;
    }

    private static Holder open(S3Wiretap.Mode mode) {
        try {
            S3Wiretap wiretap = new S3Wiretap();
            wiretap.mode(mode);
            return new Holder(wiretap);
        } catch (IOException e) {
            throw new UncheckedIOException("could not start a wiretap", e);
        }
    }

    private static final class Holder implements ExtensionContext.Store.CloseableResource {
        private final S3Wiretap wiretap;

        private Holder(S3Wiretap wiretap) {
            this.wiretap = wiretap;
        }

        public S3Wiretap wiretap() {
            return wiretap;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Holder that = (Holder) o;
            return Objects.equals(wiretap, that.wiretap);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(wiretap);
            return result;
        }

        @Override
        public String toString() {
            return "Holder[wiretap=" + wiretap + "]";
        }

        @Override
        public void close() {
            wiretap.close();
        }
    }
}

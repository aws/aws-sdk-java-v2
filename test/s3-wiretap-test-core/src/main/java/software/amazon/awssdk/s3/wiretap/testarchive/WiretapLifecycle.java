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

package software.amazon.awssdk.s3.wiretap.testarchive;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.s3.wiretap.server.S3Wiretap;

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
    }

    private static ExtensionContext containerOf(ExtensionContext context) {
        ExtensionContext current = context;
        while (current.getTestMethod().isPresent() && current.getParent().isPresent()) {
            current = current.getParent().orElseThrow();
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

    private record Holder(S3Wiretap wiretap) implements ExtensionContext.Store.CloseableResource {
        @Override
        public void close() {
            wiretap.close();
        }
    }
}

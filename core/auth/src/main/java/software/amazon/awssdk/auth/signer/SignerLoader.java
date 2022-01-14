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

package software.amazon.awssdk.auth.signer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.core.signer.Signer;

/**
 * Utility class for instantiating signers only if they're available on the class path.
 */
@SdkProtectedApi
public final class SignerLoader {

    private static final Map<String, Signer> SIGNERS = new ConcurrentHashMap<>();

    private SignerLoader() {
    }

    public static Signer getSigV4aSigner() {
        return get("software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner");
    }

    public static Signer getS3SigV4aSigner() {
        return get("software.amazon.awssdk.authcrt.signer.AwsCrtS3V4aSigner");
    }

    private static Signer get(String fqcn) {
        return SIGNERS.computeIfAbsent(fqcn, SignerLoader::initializeV4aSigner);
    }

    private static Signer initializeV4aSigner(String fqcn) {
        try {
            Class<?> signerClass = ClassLoaderHelper.loadClass(fqcn, false, (Class) null);
            Method m = signerClass.getDeclaredMethod("create");
            Object o = m.invoke(null);
            return (Signer) o;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot find the " + fqcn + " class."
                                            + " To invoke a request that requires a SigV4a signer, such as region independent " +
                                            "signing, the 'auth-crt' core module must be on the class path. ", e);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create " + fqcn, e);
        }
    }
}

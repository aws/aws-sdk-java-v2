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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.ClassLoaderHelper;

/**
 * Utility class for instantiating signers only if they're available on the class path.
 */
@SdkProtectedApi
public final class SignerLoader {

    private static final Map<String, HttpSigner<AwsCredentialsIdentity>> SIGNERS = new ConcurrentHashMap<>();

    private SignerLoader() {
    }

    public static AwsV4aHttpSigner getAwsV4aHttpSigner() {
        return get(AwsV4aHttpSigner.class, "software.amazon.awssdk.http.auth.aws.crt.internal.signer.DefaultAwsCrtV4aHttpSigner");
    }

    @SuppressWarnings("unchecked")
    private static <T extends HttpSigner<AwsCredentialsIdentity>> T get(Class<T> expectedClass, String fqcn) {
        return (T) SIGNERS.computeIfAbsent(fqcn, name -> initializeV4aSigner(expectedClass, name));
    }

    private static <T extends HttpSigner<AwsCredentialsIdentity>> T initializeV4aSigner(Class<T> expectedClass, String fqcn) {
        try {
            Class<?> signerClass = ClassLoaderHelper.loadClass(fqcn, false, (Class) null);
            Method m = signerClass.getDeclaredMethod("create");
            Object result = m.invoke(null);
            return expectedClass.cast(result);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot find the " + fqcn + " class. "
                                            + "To invoke a request that requires a SigV4a signer, such as region independent "
                                            + "signing, the 'aws-crt' core module must be on the class path.", e);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create " + fqcn, e);
        }
    }
}

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

import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.validatedProperty;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpProperties;
import software.amazon.awssdk.http.auth.aws.signer.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.Logger;

/**
 * An implementation of a {@link AwsV4HttpSigner} that uses properties to compose
 * v4-signers in order to delegate signing of a request and payload (if applicable) accordingly.
 */
@SdkProtectedApi
public final class DefaultAwsV4HttpSigner implements AwsV4HttpSigner {

    private static final Logger LOG = Logger.loggerFor(DefaultAwsV4HttpSigner.class);

    /**
     * Given a request with a set of properties, determine which signer to delegate to, and call it with the request.
     */
    private static AwsV4HttpSigner getDelegate(
        SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {

        // create the base signer
        BaseAwsV4HttpSigner<?> v4Signer = BaseAwsV4HttpSigner.create();
        return getDelegate(v4Signer, signRequest);
    }

    /**
     * Given a request with a set of properties and a base signer, compose an implementation with the base
     * signer based on properties, and delegate the request to the composed signer.
     */
    public static AwsV4HttpSigner getDelegate(
        BaseAwsV4HttpSigner<?> v4Signer,
        SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {

        // get the properties to decide on
        String authLocation = validatedProperty(signRequest, AUTH_LOCATION, "Header");
        Duration expirationDuration = validatedProperty(signRequest, EXPIRATION_DURATION, null);
        Boolean isPayloadSigning = validatedProperty(signRequest, PAYLOAD_SIGNING, true);
        Boolean isEventStreaming = validatedProperty(signRequest, EVENT_STREAMING, false);

        if (authLocation.equals("Header")) {
            v4Signer = new AwsV4HeaderHttpSigner((BaseAwsV4HttpSigner<AwsV4HttpProperties>) v4Signer);
        } else if (authLocation.equals("QueryString")) {
            v4Signer = new DefaultAwsV4QueryHttpSigner((BaseAwsV4HttpSigner<AwsV4HttpProperties>) v4Signer);
            if (expirationDuration != null) {
                v4Signer = new AwsV4PresignedHttpSigner((BaseAwsV4HttpSigner<AwsV4HttpProperties>) v4Signer);
            }
        } else {
            throw new IllegalArgumentException("Unrecognized auth-location option: " + authLocation);
        }

        if (isEventStreaming) {
            v4Signer = loadEventStreamSigner(v4Signer);
        }

        // TODO: add s3 case when migrated to auth-aws

        if (!isPayloadSigning) {
            v4Signer = new AwsV4UnsignedPayloadHttpSigner((BaseAwsV4HttpSigner<AwsV4HttpProperties>) v4Signer);
        }

        return v4Signer;
    }

    /**
     * A class-loader for the event-stream signer, which throws exceptions if it can't load the class (it's likely not on the
     * classpath, so it should be added), or if it can't instantiate the signer.
     */
    private static BaseAwsV4HttpSigner<?> loadEventStreamSigner(BaseAwsV4HttpSigner<?> v4Signer) {
        String classPath = "software.amazon.awssdk.http.auth.aws.eventstream.signer.DefaultAwsV4EventStreamHttpSigner";
        try {
            Class<?> signerClass = ClassLoaderHelper.loadClass(classPath, false);
            return (BaseAwsV4HttpSigner<?>) signerClass.getConstructor(BaseAwsV4HttpSigner.class).newInstance(v4Signer);
        } catch (ClassNotFoundException e) {
            LOG.debug(() -> "Cannot find the " + classPath + " class."
                + " To invoke a request that requires a event-streaming.", e);
            throw new RuntimeException("Event-stream signer not found. You must add a dependency on the " +
                "http-auth-aws-event-stream module to enable this functionality.");
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate the event-stream signer: ", e);
        }
    }

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        return getDelegate(request).sign(request);
    }

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        return getDelegate(request).signAsync(request);
    }
}

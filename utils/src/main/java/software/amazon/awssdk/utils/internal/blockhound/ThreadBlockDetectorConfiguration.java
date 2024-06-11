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

package software.amazon.awssdk.utils.internal.blockhound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class ThreadBlockDetectorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ThreadBlockDetectorConfiguration.class);
    private static final List<String> EXISTING_ALLOWED_BLOCKING_THREAD_PREFIXES = new ArrayList<>();
    private static final List<ThreadBlockDetector.ThreadMethodTuple> EXISTING_SYSTEM_BLOCKING_CALLS = new ArrayList<>();

    public List<String> existingAllowedBlockingThreadPrefixes() {
        return Collections.unmodifiableList(EXISTING_ALLOWED_BLOCKING_THREAD_PREFIXES);
    }

    public List<ThreadBlockDetector.ThreadMethodTuple> existingSystemBlockingCalls() {
        return Collections.unmodifiableList(EXISTING_SYSTEM_BLOCKING_CALLS);
    }

    static {
        EXISTING_ALLOWED_BLOCKING_THREAD_PREFIXES.add("idle-connection-reaper");
        EXISTING_ALLOWED_BLOCKING_THREAD_PREFIXES.add("cloud-watch-metric-publisher");
        EXISTING_ALLOWED_BLOCKING_THREAD_PREFIXES.add("sdk-cache");
        EXISTING_ALLOWED_BLOCKING_THREAD_PREFIXES.add("aws-java-sdk-NettyEventLoop");

        addAllowedBlockingCalls("*", "*", "*", "java.util.UUID.randomUUID");
        addAllowedBlockingCalls("*", "*", "*", "java.util.Random.nextInt");
        addAllowedBlockingCalls("*", "*", "*", "java.util.Random.nextLong");
        addAllowedBlockingCalls("*", "*", "*", "java.lang.Thread.sleep");
        addAllowedBlockingCalls("*", "*", "*",
                                "sun.security.provider.NativePRNG$NonBlocking.engineNextBytes");
        addAllowedBlockingCalls("*", "*", "writeBytes", "java.io");
        addAllowedBlockingCalls("*", "*", "readBytes", "java.io");
        addAllowedBlockingCalls("*", "java.lang.Object", "wait", "*");
        addAllowedBlockingCalls("ForkJoinPool.commonPool", "sun.misc.Unsafe", "park",
                                "java.util.concurrent.ForkJoinPool.awaitWork");
        addAllowedBlockingCalls("ForkJoinPool.commonPool", "java.lang.Object", "wait",
                                "java.util.concurrent.ForkJoinPool.awaitRunStateLock");
        addAllowedBlockingCalls("MetricsAggregator", "*", "*", "*");
        addAllowedBlockingCalls("*", "java.lang.Thread", "yield", "*");


        addAllowedBlockingCalls("sdk-async-response-", "java.net.SocketOutputStream", "write",
                                "software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.resolveCredentials");
        addAllowedBlockingCalls("*", "java.lang.Object", "wait",
                  "software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallMetricCollectionStage.lambda");
        addAllowedBlockingCalls("*", "java.net.SocketInputStream", "read",
                                "software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.refreshCredentials");
        addAllowedBlockingCalls("*", "java.net.Socket", "connect",
               "software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.refreshCredentials");

        addAllowedBlockingCalls("*", "java.util.concurrent.CompletableFuture", "join",
                                "ResolveEndpointInterceptor.modifyRequest");
        addAllowedBlockingCalls("*", "java.util.concurrent.CompletableFuture", "get",
                  "software.amazon.awssdk.services.s3.auth.scheme.internal.DefaultS3AuthSchemeProvider.resolveAuthScheme");
        addAllowedBlockingCalls("*", "java.util.concurrent.CompletableFuture", "get",
                  "software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyUserAgentStage.providerNameFromIdentity");
        addAllowedBlockingCalls("*", "java.util.concurrent.CompletableFuture", "get",
                                "software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.awsCredentialsReadMapping");
        addAllowedBlockingCalls("*", "java.util.concurrent.CompletableFuture", "get",
                                "software.amazon.awssdk.awscore.internal.authcontext.AwsCredentialsAuthorizationStrategy.lambda");
        addAllowedBlockingCalls("*", "java.util.concurrent.CompletableFuture", "join",
                                "software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.getProtocolNow");
        addAllowedBlockingCalls("*", "java.util.concurrent.CompletableFuture", "join",
                  "software.amazon.awssdk.core.internal.http.async.CombinedResponseAsyncHttpResponseHandler.onStream");
        addAllowedBlockingCalls("*", "java.util.concurrent.CompletableFuture", "join",
                                "software.amazon.awssdk.services.s3.internal.s3express.S3ExpressIdentityCache");
    }

    private static void addAllowedBlockingCalls(String threadPrefix, String className,
                                                String methodName, String stackTraceSegment) {
        EXISTING_SYSTEM_BLOCKING_CALLS.add(new ThreadBlockDetector.ThreadMethodTuple(
            threadPrefix, className, methodName, stackTraceSegment));
    }

    public void onBlockingMethod(ThreadBlockDetector.BlockingCallInfo blockingCallInfo) {
        if (!doNotLogMethod(blockingCallInfo)) {
            log.error(String.format("UNIDENTIFIED BLOCK DETECTED -> THREAD: %s, METHOD: %s. STACK-TRACE: %s",
                                    blockingCallInfo.threadName(),
                                    blockingCallInfo.blockingMethod().toString(),
                                    stackTraceHead(blockingCallInfo.stringifiedStackTrace(), 15)
            ));
        }
    }

    public void onAllowedBlockingMethod(ThreadBlockDetector.BlockingCallInfo blockingCallInfo) {
        if (!doNotLogMethod(blockingCallInfo)) {
            log.info(String.format("ALLOWED BLOCKING METHOD -> THREAD: %s, METHOD: %s. STACK-TRACE: %s",
                                   blockingCallInfo.threadName(),
                                   blockingCallInfo.blockingMethod().toString(),
                                   stackTraceHead(blockingCallInfo.stringifiedStackTrace(), 15)
            ));
        }
    }

    private boolean doNotLogMethod(ThreadBlockDetector.BlockingCallInfo blockingCallInfo) {
        List<String> methodsToNotLog = new ArrayList<>();
        // can be verbose, comment/uncomment as needed
        methodsToNotLog.add("readBytes");
        methodsToNotLog.add("writeBytes");
        methodsToNotLog.add("read");
        methodsToNotLog.add("write");
        methodsToNotLog.add("sleep");
        methodsToNotLog.add("park");
        //methodsToNotLog.add("wait");
        //methodsToNotLog.add("join");

        return methodsToNotLog.contains(blockingCallInfo.blockingMethod().getName());
    }

    public static String stackTraceHead(String stackTrace, int numLines) {
        String firstNLines;
        String[] lines = stackTrace.split("\n");
        firstNLines = Arrays.stream(lines).limit(numLines).map(line -> line + "\n").collect(Collectors.joining());

        return firstNLines;
    }
}

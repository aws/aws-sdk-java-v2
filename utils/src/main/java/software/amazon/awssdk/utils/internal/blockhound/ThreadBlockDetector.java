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

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingMethod;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class ThreadBlockDetector {

    private final String stackTraceDelimiter;
    private final ThreadBlockDetectorConfiguration configuration;


    public ThreadBlockDetector(ThreadBlockDetectorConfiguration configuration) {
        this.configuration = configuration;
        stackTraceDelimiter = "\n";
    }

    public void start() {
        synchronized (this) {
            BlockHound.builder()
                      .markAsBlocking(CompletableFuture.class, "join", "()Ljava/lang/Object;")
                      .markAsBlocking(CompletableFuture.class, "get", "()Ljava/lang/Object;")
                      .markAsBlocking(CompletableFuture.class, "get", "(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;")
                      .blockingMethodCallback(this::onBlockingMethod)
                      .nonBlockingThreadPredicate(current -> current.or(this::isNonBlockingThread))
                      .install();
        }
    }

    protected void onBlockingMethod(BlockingMethod blockingMethod) {
        String threadName = Thread.currentThread().getName();
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String stringifiedStackTrace = Arrays.stream(stackTraceElements).map(StackTraceElement::toString)
                                             .collect(Collectors.joining(stackTraceDelimiter));

        BlockingCallInfo info = BlockingCallInfo.builder()
                                                .blockingMethod(blockingMethod)
                                                .stackTrace(stackTraceElements)
                                                .stringifiedStackTrace(stringifiedStackTrace)
                                                .threadName(threadName)
                                                .build();

        for (ThreadMethodTuple tuple : configuration.existingSystemBlockingCalls()) {
            if (checkIfBlockingCallIsAllowed(tuple, threadName, blockingMethod, stringifiedStackTrace)) {
                this.configuration.onAllowedBlockingMethod(info);
                return;
            }
        }

        this.configuration.onBlockingMethod(info);
    }

    protected boolean checkIfBlockingCallIsAllowed(ThreadMethodTuple tuple,
                                                   String threadName,
                                                   BlockingMethod blockingMethod,
                                                   String stringifiedStackTrace) {

        boolean threadNameMatches = threadName.startsWith(tuple.threadPrefix()) || tuple.threadPrefix().equals("*");
        boolean classNameMatches = blockingMethod.getClassName().equals(tuple.className()) || tuple.className().equals("*");
        boolean methodNameMatches = blockingMethod.getName().equals(tuple.methodName()) || tuple.methodName().equals("*");
        boolean stackTraceMatches = stringifiedStackTrace.contains(tuple.stackTraceSegment())
                                    || tuple.stackTraceSegment().equals("*");

        return threadNameMatches && classNameMatches && methodNameMatches && stackTraceMatches;
    }

    protected boolean isNonBlockingThread(Thread thread) {

        // All threads marked as non-blocking by default
        return configuration.existingAllowedBlockingThreadPrefixes().stream()
                            .noneMatch(prefix -> thread.getName().startsWith(prefix));
    }

    public static final class BlockingCallInfo {
        private final BlockingMethod blockingMethod;
        private final String threadName;
        private final StackTraceElement[] stackTrace;
        private final String stringifiedStackTrace;

        private BlockingCallInfo(Builder builder) {
            this.blockingMethod = builder.blockingMethod;
            this.threadName = builder.threadName;
            this.stackTrace = builder.stackTrace;
            this.stringifiedStackTrace = builder.stringifiedStackTrace;
        }

        public static Builder builder() {
            return new Builder();
        }

        public BlockingMethod blockingMethod() {
            return blockingMethod;
        }

        public String threadName() {
            return threadName;
        }

        public StackTraceElement[] stackTrace() {
            return stackTrace.clone();
        }

        public String stringifiedStackTrace() {
            return stringifiedStackTrace;
        }

        public static class Builder {
            private BlockingMethod blockingMethod;
            private String threadName;
            private StackTraceElement[] stackTrace;
            private String stringifiedStackTrace;

            public Builder blockingMethod(BlockingMethod blockingMethod) {
                this.blockingMethod = blockingMethod;
                return this;
            }

            public Builder threadName(String threadName) {
                this.threadName = threadName;
                return this;
            }

            public Builder stackTrace(StackTraceElement[] stackTrace) {
                this.stackTrace = stackTrace.clone();
                return this;
            }

            public Builder stringifiedStackTrace(String stringifiedStackTrace) {
                this.stringifiedStackTrace = stringifiedStackTrace;
                return this;
            }

            public BlockingCallInfo build() {
                return new BlockingCallInfo(this);
            }
        }
    }

    /**
     * All the below fields allow you to either specify an exact value for the field, or a wildcard "*" to match anything.
     * In order for a tuple to match a given thread block instance, all fields must match (i.e it's an AND over all the fields,
     * not an OR).
     */
    public static class ThreadMethodTuple {
        private final String threadPrefix;
        private final String className;
        private final String methodName;
        private final String stackTraceSegment;

        ThreadMethodTuple(String threadPrefix, String className, String methodName, String stackTraceSegment) {
            this.threadPrefix = threadPrefix;
            this.className = className;
            this.methodName = methodName;
            this.stackTraceSegment = stackTraceSegment;
        }

        public String threadPrefix() {
            return threadPrefix;
        }

        public String className() {
            return className;
        }

        public String methodName() {
            return methodName;
        }

        public String stackTraceSegment() {
            return stackTraceSegment;
        }
    }
}

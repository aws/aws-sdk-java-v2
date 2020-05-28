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

package software.amazon.awssdk.stability.tests.utils;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.opentest4j.TestAbortedException;


public class RetryableTestExtension implements TestTemplateInvocationContextProvider, TestExecutionExceptionHandler {

    private static final Namespace NAMESPACE = Namespace.create(RetryableTestExtension.class);

    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        Method testMethod = extensionContext.getRequiredTestMethod();
        return isAnnotated(testMethod, RetryableTest.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        RetryExecutor executor = retryExecutor(context);
        return stream(spliteratorUnknownSize(executor, ORDERED), false);
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        if (!isRetryableException(context, throwable)) {
            throw throwable;
        }

        RetryExecutor retryExecutor = retryExecutor(context);
        retryExecutor.exceptionOccurred(throwable);
    }

    private RetryExecutor retryExecutor(ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        ExtensionContext templateContext = context.getParent()
                                                  .orElseThrow(() -> new IllegalStateException(
                                                      "Extension context \"" + context + "\" should have a parent context."));

        int maxRetries = maxRetries(context);
        return templateContext.getStore(NAMESPACE).getOrComputeIfAbsent(method.toString(), __ -> new RetryExecutor(maxRetries),
                                                                        RetryExecutor.class);


    }

    private boolean isRetryableException(ExtensionContext context, Throwable throwable) {
        return retryableException(context).isAssignableFrom(throwable.getClass()) || throwable instanceof TestAbortedException;
    }

    private int maxRetries(ExtensionContext context) {
        return retrieveAnnotation(context).maxRetries();
    }

    private Class<? extends Throwable> retryableException(ExtensionContext context) {
        return retrieveAnnotation(context).retryableException();
    }

    private RetryableTest retrieveAnnotation(ExtensionContext context) {
        return findAnnotation(context.getRequiredTestMethod(), RetryableTest.class)
            .orElseThrow(() -> new ExtensionContextException("@RetryableTest Annotation not found on method"));
    }

    private static final class RetryableTestsTemplateInvocationContext implements TestTemplateInvocationContext {
        private int maxRetries;
        private int numAttempt;

        RetryableTestsTemplateInvocationContext(int numAttempt, int maxRetries) {
            this.numAttempt = numAttempt;
            this.maxRetries = maxRetries;
        }
    }

    private static final class RetryExecutor implements Iterator<RetryableTestsTemplateInvocationContext> {
        private final int maxRetries;
        private int totalAttempts;
        private List<Throwable> throwables = new ArrayList<>();

        RetryExecutor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        private void exceptionOccurred(Throwable throwable) {
            throwables.add(throwable);
            if (throwables.size() == maxRetries) {
                throw new AssertionError("All attempts failed. Last exception: ", throwable);
            } else {
                throw new TestAbortedException(String.format("Attempt %s failed of the retryable exception, going to retry the test", totalAttempts), throwable);
            }
        }

        @Override
        public boolean hasNext() {
            if (totalAttempts == 0) {
                return true;
            }

            boolean previousFailed = totalAttempts == throwables.size();

            if (previousFailed && totalAttempts <= maxRetries - 1) {
                return true;
            }

            return false;
        }

        @Override
        public RetryableTestsTemplateInvocationContext next() {
            totalAttempts++;
            return new RetryableTestsTemplateInvocationContext(totalAttempts, maxRetries);
        }
    }
}

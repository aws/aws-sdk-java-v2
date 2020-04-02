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

package software.amazon.awssdk.testutils.retry;

import java.util.concurrent.TimeUnit;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.Validate;

public class RetryRule implements TestRule {

    private static final Logger log = LoggerFactory.getLogger(RetryRule.class);
    private int maxRetryAttempts;
    private long delay;
    private TimeUnit timeUnit;

    public RetryRule(int maxRetryAttempts) {
        this(maxRetryAttempts, 0, TimeUnit.SECONDS);
    }

    public RetryRule(int maxRetryAttempts, long delay, TimeUnit timeUnit) {
        this.maxRetryAttempts = maxRetryAttempts;
        this.delay = delay;
        this.timeUnit = Validate.paramNotNull(timeUnit, "timeUnit");
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                retry(base, 1);
            }

            void retry(final Statement base, int attempts) throws Throwable {
                try {
                    base.evaluate();
                } catch (Exception e) {
                    if (attempts > maxRetryAttempts) {
                        throw e;
                    }
                    log.warn("Test failed. Retrying with delay of: {} {}", delay, timeUnit);
                    timeUnit.sleep(delay);
                    retry(base, ++attempts);
                }
            }
        };
    }
}

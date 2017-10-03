/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.lambda.invoke;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.services.lambda.IntegrationTestBase;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.LogType;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.testutils.retry.RetryRule;

public class HelloWorldIntegrationTest extends IntegrationTestBase {

    // This is a bit ugly since it means only one person can be running
    // this test (per account) at a time, but using annotations forces us to
    // know the function name at compile time. :(
    private static final String FUNCTION_NAME = "helloWorld";
    @Rule
    public RetryRule retryRule = new RetryRule(10, 2000, TimeUnit.MILLISECONDS);
    private static HelloWorldService invoker;

    @BeforeClass
    public static void uploadFunction() throws Exception {
        // Give time for the IAM role to propagate
        System.out.println("Waiting for IAM role to propagate...");
        Thread.sleep(15_000);

        // Upload function
        byte[] functionBits;
        InputStream functionZip = new FileInputStream(cloudFuncZip);
        try {
            functionBits = read(functionZip);
        } finally {
            functionZip.close();
        }

        // Clean up any function left behind by a previous test
        deleteFunction();

        lambda.createFunction(CreateFunctionRequest.builder()
                .description("My cloud function")
                .functionName(FUNCTION_NAME)
                .code(FunctionCode.builder().zipFile(ByteBuffer.wrap(functionBits)).build())
                .handler("helloworld.handler")
                .memorySize(128)
                .runtime(Runtime.NODEJS4_3)
                .timeout(10)
                .role(lambdaServiceRoleArn)
                .build())
              .get(30, TimeUnit.SECONDS);

        invoker = LambdaInvokerFactory.builder()
                                      .lambdaClient(lambda)
                                      .build(HelloWorldService.class);

        System.out.println("Ready for testing...");
    }

    @AfterClass
    public static void deleteFunction() throws Exception {
        try {
            lambda.deleteFunction(DeleteFunctionRequest.builder().functionName(FUNCTION_NAME).build())
                  .get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // Expected, if the function doesn't exist.
        }
    }

    @Test(timeout = 30_000)
    public void test_Async() {
        // Just make sure it doesn't throw.
        invoker.helloWorldAsync();
    }

    @Test(timeout = 30_000)
    public void test_DryRun() {
        invoker.helloWorldDryRun();
    }

    @Test(timeout = 30_000)
    public void test_NoArgs() {
        Assert.assertEquals("Hello World", invoker.helloWorld());
    }

    @Test(timeout = 30_000)
    public void test_String() {
        Assert.assertEquals("Hello World", invoker.helloWorld("Testing 123"));
    }

    @Test(timeout = 30_000)
    public void test_Complex() {
        ComplexInput input = new ComplexInput();
        input.setString("Testing");
        input.setInteger(123);

        Assert.assertEquals("Hello World", invoker.helloWorld(input));
    }

    @Test(expected = LambdaSerializationException.class, timeout = 10_000)
    public void test_Bogus() {
        invoker.bogus();
    }

    @Test(timeout = 10_000)
    public void test_Failure() {
        try {
            invoker.helloWorld("BOOM");
            Assert.fail("Expected LambdaFunctionException");
        } catch (LambdaFunctionException expected) {
            expected.printStackTrace();
        }
    }

    public static interface HelloWorldService {
        @LambdaFunction(functionName = "helloWorld", invocationType = InvocationType.EVENT)
        void helloWorldAsync();

        @LambdaFunction(functionName = "helloWorld", invocationType = InvocationType.DRY_RUN)
        void helloWorldDryRun();

        @LambdaFunction
        String helloWorld();

        @LambdaFunction
        String helloWorld(String input);

        @LambdaFunction(logType = LogType.TAIL)
        String helloWorld(ComplexInput input);

        @LambdaFunction(functionName = "helloWorld")
        ComplexInput bogus();
    }

    public static class ComplexInput {

        private String string;
        private Integer integer;

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public Integer getInteger() {
            return integer;
        }

        public void setInteger(Integer integer) {
            this.integer = integer;
        }
    }
}

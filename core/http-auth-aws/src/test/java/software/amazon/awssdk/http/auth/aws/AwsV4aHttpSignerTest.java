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

package software.amazon.awssdk.http.auth.aws;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.utils.ClassLoaderHelper;

public class AwsV4aHttpSignerTest {

    @Test
    public void create_WithoutHttpAuthAwsCrtModule_throws() {
        try (MockedStatic<ClassLoaderHelper> utilities = Mockito.mockStatic(ClassLoaderHelper.class)) {
            utilities.when(() -> ClassLoaderHelper.loadClass(
                "software.amazon.awssdk.http.auth.aws.crt.HttpAuthAwsCrt",
                false)
            ).thenThrow(new ClassNotFoundException("boom!"));
            Exception e = assertThrows(RuntimeException.class, AwsV4aHttpSigner::create);
            AssertionsForClassTypes.assertThat(e).hasMessageContaining("http-auth-aws-crt");
        }
    }
}

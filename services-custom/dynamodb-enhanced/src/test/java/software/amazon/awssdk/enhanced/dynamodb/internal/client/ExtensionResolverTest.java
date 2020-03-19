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

package software.amazon.awssdk.enhanced.dynamodb.internal.client;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionResolverTest {
    @Mock
    private DynamoDbEnhancedClientExtension mockExtension1;
    @Mock
    private DynamoDbEnhancedClientExtension mockExtension2;

    @Before
    public void stubMocks() {
        when(mockExtension1.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(WriteModification.builder().build());
        when(mockExtension2.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class))).thenReturn(WriteModification.builder().build());
        when(mockExtension1.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(ReadModification.builder().build());
        when(mockExtension2.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(ReadModification.builder().build());
    }

    @Test
    public void resolveExtensions_null() {
        assertThat(ExtensionResolver.resolveExtensions(null)).isNull();
    }

    @Test
    public void resolveExtensions_empty() {
        assertThat(ExtensionResolver.resolveExtensions(emptyList())).isNull();
    }

    @Test
    public void resolveExtensions_singleton() {
        assertThat(ExtensionResolver.resolveExtensions(singletonList(mockExtension1))).isSameAs(mockExtension1);
    }

    @Test
    public void resolveExtensions_multiple_beforeWrite_correctCallingOrder() {
        DynamoDbEnhancedClientExtension extension =
            ExtensionResolver.resolveExtensions(Arrays.asList(mockExtension1, mockExtension2));

        extension.beforeWrite(mock(DynamoDbExtensionContext.BeforeWrite.class));
        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2);
        inOrder.verify(mockExtension1).beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class));
        inOrder.verify(mockExtension2).beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void resolveExtensions_multiple_afterRead_correctCallingOrder() {
        DynamoDbEnhancedClientExtension extension =
            ExtensionResolver.resolveExtensions(Arrays.asList(mockExtension1, mockExtension2));

        extension.afterRead(mock(DynamoDbExtensionContext.AfterRead.class));
        InOrder inOrder = Mockito.inOrder(mockExtension1, mockExtension2);
        inOrder.verify(mockExtension2).afterRead(any(DynamoDbExtensionContext.AfterRead.class));
        inOrder.verify(mockExtension1).afterRead(any(DynamoDbExtensionContext.AfterRead.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void defaultExtensions_isImmutable() {
        List<DynamoDbEnhancedClientExtension> defaultExtensions = ExtensionResolver.defaultExtensions();
        assertThatThrownBy(() -> defaultExtensions.add(mockExtension1)).isInstanceOf(UnsupportedOperationException.class);
    }
}

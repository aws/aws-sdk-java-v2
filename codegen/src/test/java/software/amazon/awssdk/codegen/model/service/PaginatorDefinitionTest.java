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

package software.amazon.awssdk.codegen.model.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PaginatorDefinitionTest {

    @Test
    public void isValid_ReturnsFalse_WhenInputToken_IsNullOrEmpty() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();

        Assertions.assertNull(paginatorDefinition.getInputToken());
        Assertions.assertFalse(paginatorDefinition.isValid());

        paginatorDefinition.setInputToken(Collections.emptyList());
        Assertions.assertFalse(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsFalse_WhenOutputToken_IsNullOrEmpty() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();

        Assertions.assertNull(paginatorDefinition.getOutputToken());
        Assertions.assertFalse(paginatorDefinition.isValid());

        paginatorDefinition.setOutputToken(Collections.emptyList());
        Assertions.assertFalse(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsFalse_WhenInputTokenIsPresent_AndOutputTokenIsMissing() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();
        paginatorDefinition.setInputToken(Arrays.asList("inputToken"));

        Assertions.assertFalse(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsTrue_WhenBothInputTokenAndOutputToken_ArePresent() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();
        paginatorDefinition.setInputToken(Arrays.asList("inputToken"));
        paginatorDefinition.setOutputToken(Arrays.asList("foo", "bar"));

        Assertions.assertTrue(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsTrue_WhenResultKey_IsNull() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();
        paginatorDefinition.setInputToken(Arrays.asList("inputToken"));
        paginatorDefinition.setOutputToken(Arrays.asList("foo", "bar"));

        Assertions.assertNull(paginatorDefinition.getResultKey());
        Assertions.assertTrue(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsTrue_WhenOutputTokenList_ContainsValidString() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();
        paginatorDefinition.setOutputToken(Arrays.asList("Foo", "Foo.Bar"));
        paginatorDefinition.setInputToken(Arrays.asList("token"));

        Assertions.assertTrue(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsFalse_WhenOutputTokenList_ContainsAListMember() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();
        paginatorDefinition.setOutputToken(Arrays.asList("Contents[-1]"));
        paginatorDefinition.setInputToken(Arrays.asList("token"));

        Assertions.assertFalse(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsTrue_WhenOutputTokenList_ContainsOrCharacter() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();
        paginatorDefinition.setOutputToken(Arrays.asList("NextMarker || Contents"));
        paginatorDefinition.setInputToken(Arrays.asList("token"));

        Assertions.assertFalse(paginatorDefinition.isValid());
    }
}

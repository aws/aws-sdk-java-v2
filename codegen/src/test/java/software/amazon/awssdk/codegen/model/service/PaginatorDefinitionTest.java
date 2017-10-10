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

package software.amazon.awssdk.codegen.model.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class PaginatorDefinitionTest {

    @Test
    public void isValid_ReturnsFalse_WhenInputToken_IsNullOrEmpty() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();

        assertNull(paginatorDefinition.getInputToken());
        assertFalse(paginatorDefinition.isValid());

        paginatorDefinition.setInputToken(Collections.emptyList());
        assertFalse(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsFalse_WhenOutputToken_IsNullOrEmpty() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();

        assertNull(paginatorDefinition.getOutputToken());
        assertFalse(paginatorDefinition.isValid());

        paginatorDefinition.setOutputToken(Collections.emptyList());
        assertFalse(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsFalse_WhenInputTokenIsPresent_AndOutputTokenIsMissing() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();
        paginatorDefinition.setInputToken(Arrays.asList("input-token"));

        assertFalse(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsTrue_WhenBothInputTokenAndOutputToken_ArePresent() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();
        paginatorDefinition.setInputToken(Arrays.asList("input-token"));
        paginatorDefinition.setOutputToken(Arrays.asList("token1", "token2"));

        assertTrue(paginatorDefinition.isValid());
    }

    @Test
    public void isValid_ReturnsTrue_WhenResultKey_IsNull() {
        PaginatorDefinition paginatorDefinition = new PaginatorDefinition();
        paginatorDefinition.setInputToken(Arrays.asList("input-token"));
        paginatorDefinition.setOutputToken(Arrays.asList("token1", "token2"));

        assertNull(paginatorDefinition.getResultKey());
        assertTrue(paginatorDefinition.isValid());
    }
}

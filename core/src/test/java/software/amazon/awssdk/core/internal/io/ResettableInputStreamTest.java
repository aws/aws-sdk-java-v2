/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.core.util.FileUtils.generateRandomAsciiFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.runtime.io.ReleasableInputStream;
import software.amazon.awssdk.core.runtime.io.ResettableInputStream;

public class ResettableInputStreamTest {

    private static File file;

    @BeforeClass
    public static void setup() throws IOException {
        file = generateRandomAsciiFile(100);
    }

    @Test
    public void testFileInputStream() throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            assertFalse(is.markSupported());
            String content = IOUtils.toString(is);
            String content2 = IOUtils.toString(is);
            assertTrue(content.length() == 100);
            assertEquals(content2, "");
        }
    }

    @Test
    public void testResetInputStreamWithFile() throws IOException {
        try (ResettableInputStream is = new ResettableInputStream(file)) {
            assertTrue(is.markSupported());
            String content = IOUtils.toString(is);
            is.reset();
            String content2 = IOUtils.toString(is);
            assertTrue(content.length() == 100);
            assertEquals(content, content2);
            assertEquals(file, is.getFile());
        }
    }

    @Test
    public void testResetFileInputStream() throws IOException {
        try (ResettableInputStream is = new ResettableInputStream(
            new FileInputStream(file))) {
            assertTrue(is.markSupported());
            String content = IOUtils.toString(is);
            is.reset();
            String content2 = IOUtils.toString(is);
            assertTrue(content.length() == 100);
            assertEquals(content, content2);
            assertNull(is.getFile());
        }
    }

    @Test
    public void testMarkAndResetWithFile() throws IOException {
        try (ResettableInputStream is = new ResettableInputStream(file)) {
            is.read(new byte[10]);
            is.mark(-1);
            String content = IOUtils.toString(is);
            is.reset();
            String content2 = IOUtils.toString(is);
            assertTrue(content.length() == 90);
            assertEquals(content, content2);
        }
    }

    @Test
    public void testMarkAndResetFileInputStream() throws IOException {
        try (ResettableInputStream is = new ResettableInputStream(new FileInputStream(file))) {
            is.read(new byte[10]);
            is.mark(-1);
            String content = IOUtils.toString(is);
            is.reset();
            String content2 = IOUtils.toString(is);
            assertTrue(content.length() == 90);
            assertEquals(content, content2);
        }
    }

    @Test
    public void testResetWithClosedFile() throws IOException {
        ResettableInputStream is = null;
        try {
            is = new ResettableInputStream(file).disableClose();
            String content = IOUtils.toString(is);
            is.close();
            is.reset(); // survive a close operation!
            String content2 = IOUtils.toString(is);
            assertTrue(content.length() == 100);
            assertEquals(content, content2);
        } finally {
            Optional.ofNullable(is).ifPresent(ReleasableInputStream::release);
        }
    }

    @Test(expected = ClosedChannelException.class)
    public void negativeTestResetWithClosedFile() throws IOException {
        try (ResettableInputStream is = new ResettableInputStream(file)) {
            is.close();
            is.reset();
        }
    }

    @Test
    public void testMarkAndResetWithClosedFile() throws IOException {
        ResettableInputStream is = null;
        try {
            is = new ResettableInputStream(file).disableClose();
            is.read(new byte[10]);
            is.mark(-1);
            String content = IOUtils.toString(is);
            is.close();
            is.reset(); // survive a close operation!
            String content2 = IOUtils.toString(is);
            assertTrue(content.length() == 90);
            assertEquals(content, content2);
        } finally {
            Optional.ofNullable(is).ifPresent(ReleasableInputStream::release);
        }
    }

    @Test(expected = ClosedChannelException.class)
    public void testMarkAndResetClosedFileInputStream() throws IOException {
        try (ResettableInputStream is = new ResettableInputStream(new FileInputStream(file))) {
            is.close();
            is.reset(); // cannot survive a close if not disabled
        }
    }
}

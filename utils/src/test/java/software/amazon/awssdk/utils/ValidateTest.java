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

package software.amazon.awssdk.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests {@link Validate}.
 *
 * Adapted from https://github.com/apache/commons-lang.
 */
public class ValidateTest  {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    //-----------------------------------------------------------------------
    @Test
    public void testIsTrue2() {
        Validate.isTrue(true, "MSG");
        try {
            Validate.isTrue(false, "MSG");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void testIsTrue3() {
        Validate.isTrue(true, "MSG", 6);
        try {
            Validate.isTrue(false, "MSG", 6);
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void testIsTrue4() {
        Validate.isTrue(true, "MSG", 7);
        try {
            Validate.isTrue(false, "MSG", 7);
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void testIsTrue5() {
        Validate.isTrue(true, "MSG", 7.4d);
        try {
            Validate.isTrue(false, "MSG", 7.4d);
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    @SuppressWarnings("unused")
    @Test
    public void testNotNull2() {
        Validate.notNull(new Object(), "MSG");
        try {
            Validate.notNull(null, "MSG");
            fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            assertEquals("MSG", ex.getMessage());
        }

        final String str = "Hi";
        final String testStr = Validate.notNull(str, "Message");
        assertSame(str, testStr);
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotEmptyArray2() {
        Validate.notEmpty(new Object[] {null}, "MSG");
        try {
            Validate.notEmpty((Object[]) null, "MSG");
            fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            assertEquals("MSG", ex.getMessage());
        }
        try {
            Validate.notEmpty(new Object[0], "MSG");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }

        final String[] array = new String[] {"hi"};
        final String[] test = Validate.notEmpty(array, "Message");
        assertSame(array, test);
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotEmptyCollection2() {
        final Collection<Integer> coll = new ArrayList<>();
        try {
            Validate.notEmpty((Collection<?>) null, "MSG");
            fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            assertEquals("MSG", ex.getMessage());
        }
        try {
            Validate.notEmpty(coll, "MSG");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }
        coll.add(8);
        Validate.notEmpty(coll, "MSG");

        final Collection<Integer> test = Validate.notEmpty(coll, "Message");
        assertSame(coll, test);
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotEmptyMap2() {
        final Map<String, Integer> map = new HashMap<>();
        try {
            Validate.notEmpty((Map<?, ?>) null, "MSG");
            fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            assertEquals("MSG", ex.getMessage());
        }
        try {
            Validate.notEmpty(map, "MSG");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }
        map.put("ll", 8);
        Validate.notEmpty(map, "MSG");

        final Map<String, Integer> test = Validate.notEmpty(map, "Message");
        assertSame(map, test);
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotEmptyString2() {
        Validate.notEmpty("a", "MSG");
        try {
            Validate.notEmpty((String) null, "MSG");
            fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            assertEquals("MSG", ex.getMessage());
        }
        try {
            Validate.notEmpty("", "MSG");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }

        final String str = "Hi";
        final String testStr = Validate.notEmpty(str, "Message");
        assertSame(str, testStr);
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotBlankMsgNullStringShouldThrow() {
        //given
        final String string = null;

        try {
            //when
            Validate.notBlank(string, "Message");
            fail("Expecting NullPointerException");
        } catch (final NullPointerException e) {
            //then
            assertEquals("Message", e.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotBlankMsgBlankStringShouldThrow() {
        //given
        final String string = " \n \t \r \n ";

        try {
            //when
            Validate.notBlank(string, "Message");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            //then
            assertEquals("Message", e.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotBlankMsgBlankStringWithWhitespacesShouldThrow() {
        //given
        final String string = "   ";

        try {
            //when
            Validate.notBlank(string, "Message");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            //then
            assertEquals("Message", e.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotBlankMsgEmptyStringShouldThrow() {
        //given
        final String string = "";

        try {
            //when
            Validate.notBlank(string, "Message");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            //then
            assertEquals("Message", e.getMessage());
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotBlankMsgNotBlankStringShouldNotThrow() {
        //given
        final String string = "abc";

        //when
        Validate.notBlank(string, "Message");

        //then should not throw
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotBlankMsgNotBlankStringWithWhitespacesShouldNotThrow() {
        //given
        final String string = "  abc   ";

        //when
        Validate.notBlank(string, "Message");

        //then should not throw
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNotBlankMsgNotBlankStringWithNewlinesShouldNotThrow() {
        //given
        final String string = " \n \t abc \r \n ";

        //when
        Validate.notBlank(string, "Message");

        //then should not throw
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNoNullElementsArray2() {
        String[] array = new String[] {"a", "b"};
        Validate.noNullElements(array, "MSG");
        try {
            Validate.noNullElements((Object[]) null, "MSG");
            fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            assertEquals("MSG", ex.getMessage());
        }
        array[1] = null;
        try {
            Validate.noNullElements(array, "MSG");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }

        array = new String[] {"a", "b"};
        final String[] test = Validate.noNullElements(array, "Message");
        assertSame(array, test);
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNoNullElementsCollection2() {
        final List<String> coll = new ArrayList<>();
        coll.add("a");
        coll.add("b");
        Validate.noNullElements(coll, "MSG");
        try {
            Validate.noNullElements((Collection<?>) null, "MSG");
            fail("Expecting NullPointerException");
        } catch (final NullPointerException ex) {
            assertEquals("The validated object is null", ex.getMessage());
        }
        coll.set(1, null);
        try {
            Validate.noNullElements(coll, "MSG");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
            assertEquals("MSG", ex.getMessage());
        }

        coll.set(1, "b");
        final List<String> test = Validate.noNullElements(coll, "Message");
        assertSame(coll, test);
    }

    @Test
    public void testInclusiveBetween_withMessage()
    {
        Validate.inclusiveBetween("a", "c", "b", "Error");
        try {
            Validate.inclusiveBetween("0", "5", "6", "Error");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
    }

    @Test
    public void testInclusiveBetweenLong_withMessage()
    {
        Validate.inclusiveBetween(0, 2, 1, "Error");
        Validate.inclusiveBetween(0, 2, 2, "Error");
        try {
            Validate.inclusiveBetween(0, 5, 6, "Error");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
    }

    @Test
    public void testInclusiveBetweenDouble_withMessage()
    {
        Validate.inclusiveBetween(0.1, 2.1, 1.1, "Error");
        Validate.inclusiveBetween(0.1, 2.1, 2.1, "Error");
        try {
            Validate.inclusiveBetween(0.1, 5.1, 6.1, "Error");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
    }

    @Test
    public void testExclusiveBetween_withMessage()
    {
        Validate.exclusiveBetween("a", "c", "b", "Error");
        try {
            Validate.exclusiveBetween("0", "5", "6", "Error");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
        try {
            Validate.exclusiveBetween("0", "5", "5", "Error");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
    }

    @Test
    public void testExclusiveBetweenLong_withMessage()
    {
        Validate.exclusiveBetween(0, 2, 1, "Error");
        try {
            Validate.exclusiveBetween(0, 5, 6, "Error");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
        try {
            Validate.exclusiveBetween(0, 5, 5, "Error");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
    }

    @Test
    public void testExclusiveBetweenDouble_withMessage()
    {
        Validate.exclusiveBetween(0.1, 2.1, 1.1, "Error");
        try {
            Validate.exclusiveBetween(0.1, 5.1, 6.1, "Error");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
        try {
            Validate.exclusiveBetween(0.1, 5.1, 5.1, "Error");
            fail("Expecting IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
    }

    @Test
    public void testIsInstanceOf_withMessage() {
        Validate.isInstanceOf(String.class, "hi", "Error");
        Validate.isInstanceOf(Integer.class, 1, "Error");
        try {
            Validate.isInstanceOf(List.class, "hi", "Error");
            fail("Expecting IllegalArgumentException");
        } catch(final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
    }

    @Test
    public void testIsInstanceOf_withMessageArgs() {
        Validate.isInstanceOf(String.class, "hi", "Error %s=%s", "Name", "Value");
        Validate.isInstanceOf(Integer.class, 1, "Error %s=%s", "Name", "Value");
        try {
            Validate.isInstanceOf(List.class, "hi", "Error %s=%s", "Name", "Value");
            fail("Expecting IllegalArgumentException");
        } catch(final IllegalArgumentException e) {
            assertEquals("Error Name=Value", e.getMessage());
        }
        try {
            Validate.isInstanceOf(List.class, "hi", "Error %s=%s", List.class, "Value");
            fail("Expecting IllegalArgumentException");
        } catch(final IllegalArgumentException e) {
            assertEquals("Error interface java.util.List=Value", e.getMessage());
        }
        try {
            Validate.isInstanceOf(List.class, "hi", "Error %s=%s", List.class, null);
            fail("Expecting IllegalArgumentException");
        } catch(final IllegalArgumentException e) {
            assertEquals("Error interface java.util.List=null", e.getMessage());
        }
    }

    @Test
    public void testIsAssignable_withMessage() {
        Validate.isAssignableFrom(CharSequence.class, String.class, "Error");
        Validate.isAssignableFrom(AbstractList.class, ArrayList.class, "Error");
        try {
            Validate.isAssignableFrom(List.class, String.class, "Error");
            fail("Expecting IllegalArgumentException");
        } catch(final IllegalArgumentException e) {
            assertEquals("Error", e.getMessage());
        }
    }

    @Test
    public void paramNotNull_NullParam_ThrowsException() {
        try {
            Validate.paramNotNull(null, "someField");
        } catch (NullPointerException e) {
            assertEquals(e.getMessage(), "someField must not be null.");
        }
    }

    @Test
    public void paramNotNull_NonNullParam_ReturnsObject() {
        assertEquals("foo", Validate.paramNotNull("foo", "someField"));
    }

    @Test
    public void getOrDefault_ParamNotNull_ReturnsParam() {
        assertEquals("foo", Validate.getOrDefault("foo", () -> "bar"));
    }

    @Test
    public void getOrDefault_ParamNull_ReturnsDefaultValue() {
        assertEquals("bar", Validate.getOrDefault(null, () -> "bar"));
    }

    @Test(expected = NullPointerException.class)
    public void getOrDefault_DefaultValueNull_ThrowsException() {
        Validate.getOrDefault("bar", null);
    }

    @Test
    public void mutuallyExclusive_AllNull_DoesNotThrow() {
        Validate.mutuallyExclusive("error", null, null, null);
    }

    @Test
    public void mutuallyExclusive_OnlyOneProvided_DoesNotThrow() {
        Validate.mutuallyExclusive("error", null, "foo", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mutuallyExclusive_MultipleProvided_DoesNotThrow() {
        Validate.mutuallyExclusive("error", null, "foo", "bar");
    }

    @Test
    public void isPositiveOrNullInteger_null_returnsNull() {
        assertNull(Validate.isPositiveOrNull((Integer) null, "foo"));
    }

    @Test
    public void isPositiveOrNullInteger_positive_returnsInteger() {
        Integer num = 42;
        assertEquals(num, Validate.isPositiveOrNull(num, "foo"));
    }

    @Test
    public void isPositiveOrNullInteger_zero_throws() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("foo");
        Validate.isPositiveOrNull(0, "foo");
    }

    @Test
    public void isPositiveOrNullInteger_negative_throws() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("foo");
        Validate.isPositiveOrNull(-1, "foo");
    }

    @Test
    public void isPositiveOrNullLong_null_returnsNull() {
        assertNull(Validate.isPositiveOrNull((Long) null, "foo"));
    }

    @Test
    public void isPositiveOrNullLong_positive_returnsInteger() {
        Long num = 42L;
        assertEquals(num, Validate.isPositiveOrNull(num, "foo"));
    }

    @Test
    public void isPositiveOrNullLong_zero_throws() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("foo");
        Validate.isPositiveOrNull(0L, "foo");
    }

    @Test
    public void isPositiveOrNullLong_negative_throws() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("foo");
        Validate.isPositiveOrNull(-1L, "foo");
    }
}

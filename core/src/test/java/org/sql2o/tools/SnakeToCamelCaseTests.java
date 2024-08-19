package org.sql2o.tools;

import junit.framework.TestCase;

public class SnakeToCamelCaseTests extends TestCase {

	public void testBasicConversions() {
		assertEquals("myStringVariable", SnakeToCamelCase.convert("my_string_variable"));
		assertEquals("string", SnakeToCamelCase.convert("string"));
		assertEquals("myReallyLongStringVariableName", SnakeToCamelCase.convert("my_really_long_string_variable_name"));
		assertEquals("myString2WithNumbers4", SnakeToCamelCase.convert("my_string2_with_numbers_4"));
		assertEquals("myStringWithMixedCase", SnakeToCamelCase.convert("my_string_with_MixED_CaSe"));
	}
	
	public void testNullString() {
		assertNull(SnakeToCamelCase.convert(null));
	}
	
	public void testEmptyStrings() {
		assertEquals("", SnakeToCamelCase.convert(""));
		assertEquals(" ", SnakeToCamelCase.convert(" "));
	}
	public void testWhitespace() {
		assertEquals("\t", SnakeToCamelCase.convert("\t"));
		assertEquals("\n\n", SnakeToCamelCase.convert("\n\n"));
	}
}

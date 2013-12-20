package org.sql2o.tools;

import junit.framework.TestCase;

public class UnderscoreToCamelCaseTests extends TestCase {

	public void testBasicConversions() {
		assertEquals("myStringVariable", UnderscoreToCamelCase.convert("my_string_variable"));
		assertEquals("string", UnderscoreToCamelCase.convert("string"));
		assertEquals("myReallyLongStringVariableName", UnderscoreToCamelCase.convert("my_really_long_string_variable_name"));
		assertEquals("myString2WithNumbers4", UnderscoreToCamelCase.convert("my_string2_with_numbers_4"));
		assertEquals("myStringWithMixedCase", UnderscoreToCamelCase.convert("my_string_with_MixED_CaSe"));
	}
	
	public void testNullString() {
		assertNull(UnderscoreToCamelCase.convert(null));
	}
	
	public void testEmptyStrings() {
		assertEquals("", UnderscoreToCamelCase.convert(""));
		assertEquals(" ", UnderscoreToCamelCase.convert(" "));
	}
	public void testWhitespace() {
		assertEquals("\t", UnderscoreToCamelCase.convert("\t"));
		assertEquals("\n\n", UnderscoreToCamelCase.convert("\n\n"));
	}
}

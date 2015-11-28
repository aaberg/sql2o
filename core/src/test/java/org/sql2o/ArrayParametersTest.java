package org.sql2o;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;

public class ArrayParametersTest {

	@Test
	public void testUpdateQueryWithArrayParameters() {
		assertEquals(
				"SELECT * FROM user WHERE id IN(?,?,?,?,?)",
				ArrayParameters.updateQueryWithArrayParameters(
						"SELECT * FROM user WHERE id IN(?)",
						Arrays.asList(new ArrayParameters.ArrayParameter(1, 5))
				)
		);

		assertEquals(
				"SELECT * FROM user WHERE id IN(?)",
				ArrayParameters.updateQueryWithArrayParameters(
						"SELECT * FROM user WHERE id IN(?)",
						new ArrayList<ArrayParameters.ArrayParameter>()
				)
		);

		assertEquals(
				"SELECT * FROM user WHERE id IN(?)",
				ArrayParameters.updateQueryWithArrayParameters(
						"SELECT * FROM user WHERE id IN(?)",
						Arrays.asList(new ArrayParameters.ArrayParameter(1, 0))
				)
		);

		assertEquals(
				"SELECT * FROM user WHERE id IN(?)",
				ArrayParameters.updateQueryWithArrayParameters(
						"SELECT * FROM user WHERE id IN(?)",
						Arrays.asList(new ArrayParameters.ArrayParameter(1, 1))
				)
		);

		assertEquals(
				"SELECT * FROM user WHERE login = ? AND id IN(?,?)",
				ArrayParameters.updateQueryWithArrayParameters(
						"SELECT * FROM user WHERE login = ? AND id IN(?)",
						Arrays.asList(new ArrayParameters.ArrayParameter(2, 2))
				)
		);

		assertEquals(
				"SELECT * FROM user WHERE login = ? AND id IN(?,?) AND name = ?",
				ArrayParameters.updateQueryWithArrayParameters(
						"SELECT * FROM user WHERE login = ? AND id IN(?) AND name = ?",
						Arrays.asList(new ArrayParameters.ArrayParameter(2, 2))
				)
		);

		assertEquals(
				"SELECT ... WHERE other_id IN (?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
				ArrayParameters.updateQueryWithArrayParameters(
						"SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
						Arrays.asList(
								new ArrayParameters.ArrayParameter(1, 3),
								new ArrayParameters.ArrayParameter(3, 3)
						)
				)
		);

		assertEquals(
				"SELECT ... WHERE other_id IN (?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
				ArrayParameters.updateQueryWithArrayParameters(
						"SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
						Arrays.asList(
								new ArrayParameters.ArrayParameter(1, 3),
								new ArrayParameters.ArrayParameter(3, 3)
						)
				)
		);

		assertEquals(
				"SELECT ... WHERE other_id IN (?,?,?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
				ArrayParameters.updateQueryWithArrayParameters(
						"SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
						Arrays.asList(
								new ArrayParameters.ArrayParameter(1, 5),
								new ArrayParameters.ArrayParameter(3, 3)
						)
				)
		);
	}

}

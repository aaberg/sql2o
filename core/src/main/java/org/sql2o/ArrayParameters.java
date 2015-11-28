package org.sql2o;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class ArrayParameters {

	static class ArrayParameter {
		// the index of the parameter array
		int parameterIndex;
		// the number of parameters to put in the query placeholder
		int parameterCount;

		ArrayParameter(int parameterIndex, int parameterCount) {
			this.parameterIndex = parameterIndex;
			this.parameterCount = parameterCount;
		}
	}

	/**
	 * Change the query to replace ? at each arrayParameters.parameterIndex
	 * with ?,?,?.. multiple arrayParameters.parameterCount
	 */
	static String updateQueryWithArrayParameters(String parsedQuery, List<ArrayParameter> arrayParameters) {
		if(arrayParameters.isEmpty()) {
			return parsedQuery;
		}

		StringBuilder sb = new StringBuilder();

		Collections.sort(arrayParameters, new Comparator<ArrayParameter>() {
			@Override
			public int compare(ArrayParameter o1, ArrayParameter o2) {
				return Integer.compare(o1.parameterIndex, o2.parameterIndex);
			}
		});

		Iterator<ArrayParameter> parameterToReplaceIt = arrayParameters.iterator();
		ArrayParameter nextParameterToReplace = parameterToReplaceIt.next();
		// PreparedStatement index starts at 1
		int currentIndex = 1;
		for(char c : parsedQuery.toCharArray()) {
			if(nextParameterToReplace != null && c == '?') {
				if(currentIndex == nextParameterToReplace.parameterIndex) {
					sb.append("?");
					for(int i = 1; i < nextParameterToReplace.parameterCount; i++) {
						sb.append(",?");
					}

					if(parameterToReplaceIt.hasNext()) {
						nextParameterToReplace = parameterToReplaceIt.next();
					} else {
						nextParameterToReplace = null;
					}
				} else {
					sb.append(c);
				}
				currentIndex++;
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

}

package org.sql2o;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ArrayParameters {

    static class ArrayParameter implements Comparable<ArrayParameter> {
        // the index of the parameter array
        int parameterIndex;
        // the number of parameters to put in the query placeholder
        int parameterCount;

        ArrayParameter(int parameterIndex, int parameterCount) {
            this.parameterIndex = parameterIndex;
            this.parameterCount = parameterCount;
        }

        @Override
        public int compareTo(ArrayParameter o) {
            return Integer.compare(parameterIndex, o.parameterIndex);
        }
    }

    /**
     * Update both the query and the parameter indexes to include the array parameters.
     */
    static String updateQueryAndParametersIndexes(String parsedQuery,
                                                  Map<String, List<Integer>> parameterNamesToIndexes,
                                                  Map<String, Query.ParameterSetter> parameters,
                                                  boolean allowArrayParameters) {
        List<ArrayParameter> arrayParametersSortedAsc = arrayParametersSortedAsc(parameterNamesToIndexes, parameters, allowArrayParameters);
        if(arrayParametersSortedAsc.isEmpty()) {
            return parsedQuery;
        }

        updateParameterNamesToIndexes(parameterNamesToIndexes, arrayParametersSortedAsc);

        return updateQueryWithArrayParameters(parsedQuery, arrayParametersSortedAsc);
    }

    /**
     * Update the indexes of each query parameter
     */
    static Map<String, List<Integer>> updateParameterNamesToIndexes(Map<String, List<Integer>> parametersNameToIndex,
                                                                    List<ArrayParameter> arrayParametersSortedAsc) {
        for(Map.Entry<String, List<Integer>> parameterNameToIndexes : parametersNameToIndex.entrySet()) {
            List<Integer> newParameterIndex = new ArrayList<>(parameterNameToIndexes.getValue().size());
            for(Integer parameterIndex : parameterNameToIndexes.getValue()) {
                newParameterIndex.add(computeNewIndex(parameterIndex, arrayParametersSortedAsc));
            }
            parameterNameToIndexes.setValue(newParameterIndex);
        }

        return parametersNameToIndex;
    }


    /**
     * Compute the new index of a parameter given the index positions of the array parameters.
     */
    static int computeNewIndex(int index, List<ArrayParameter> arrayParametersSortedAsc) {
        int newIndex = index;
        for(ArrayParameter arrayParameter : arrayParametersSortedAsc) {
            if(index > arrayParameter.parameterIndex) {
                newIndex = newIndex + arrayParameter.parameterCount - 1;
            } else {
                return newIndex;
            }
        }
        return newIndex;
    }

    /**
     * List all the array parameters that contains more that 1 parameters.
     * Indeed, array parameter below 1 parameter will not change the text query nor the parameter indexes.
     */
    private static List<ArrayParameter> arrayParametersSortedAsc(Map<String, List<Integer>> parameterNamesToIndexes,
                                                                 Map<String, Query.ParameterSetter> parameters,
                                                                 boolean allowArrayParameters) {
        List<ArrayParameters.ArrayParameter> arrayParameters = new ArrayList<>();
        for(Map.Entry<String, Query.ParameterSetter> parameter : parameters.entrySet()) {
            if (parameter.getValue().parameterCount > 1) {
                if (!allowArrayParameters) {
                    throw new Sql2oException("Array parameters are not allowed in batch mode");
                }
                for(int i : parameterNamesToIndexes.get(parameter.getKey())) {
                    arrayParameters.add(new ArrayParameters.ArrayParameter(i, parameter.getValue().parameterCount));
                }
            }
        }
        Collections.sort(arrayParameters);

        return arrayParameters;
    }

    /**
     * Change the query to replace ? at each arrayParametersSortedAsc.parameterIndex
     * with ?,?,?.. multiple arrayParametersSortedAsc.parameterCount
     */
    static String updateQueryWithArrayParameters(String parsedQuery, List<ArrayParameter> arrayParametersSortedAsc) {
        if(arrayParametersSortedAsc.isEmpty()) {
            return parsedQuery;
        }

        StringBuilder sb = new StringBuilder();

        Iterator<ArrayParameter> parameterToReplaceIt = arrayParametersSortedAsc.iterator();
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

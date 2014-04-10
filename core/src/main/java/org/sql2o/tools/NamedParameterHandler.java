package org.sql2o.tools;

/**
 * Created by lars on 10.04.14.
 */
public interface NamedParameterHandler {

    int[] getParameterIndices(String name);

    boolean containsParameter(String name);

    String parseStatement(String statement);
}

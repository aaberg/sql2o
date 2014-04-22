package org.sql2o;

import org.sql2o.tools.OutParameterGetter;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * Created by lars on 16.04.14.
 */
public class OutParameter<T> {

    private final String name;
    private T value;

    private OutParameterGetter<T> outParameterGetter;
    private Class paramGetterClass;
    private final RegisterParam registerParam;

    public OutParameter(String name, final int type, OutParameterGetter outParameterGetter) {
        registerParam = new RegisterParam() {
            public void registerOutParam(int paramIdx, CallableStatement statement) throws SQLException {
                statement.registerOutParameter(paramIdx, type);
            }
        };
        this.name = name;
        this.outParameterGetter = outParameterGetter;
    }

    public OutParameter(String name, final int type, final int scale, OutParameterGetter outParameterGetter) {
        registerParam = new RegisterParam() {
            public void registerOutParam(int paramIdx, CallableStatement statement) throws SQLException {
                statement.registerOutParameter(paramIdx, type, scale);
            }
        };
        this.name = name;
        this.outParameterGetter = outParameterGetter;
    }

    public OutParameter(String name, final int type, final String typeName, OutParameterGetter outParameterGetter) {
        registerParam = new RegisterParam() {
            public void registerOutParam(int paramIdx, CallableStatement statement) throws SQLException {
                statement.registerOutParameter(paramIdx, type, typeName);
            }
        };
        this.name = name;
        this.outParameterGetter = outParameterGetter;
    }

    public OutParameter(String name, int sqlType, Class outputType) {
        this(name, sqlType, (OutParameterGetter)null);
        paramGetterClass = outputType;
    }

    public OutParameter(String name, int sqlType, int scale, Class outputType){
        this(name, sqlType, scale, (OutParameterGetter)null);
        paramGetterClass = outputType;
    }

    public OutParameter(String name, int sqlType, String typeName, Class outputType) {
        this(name, sqlType, typeName, (OutParameterGetter)null);
        paramGetterClass = outputType;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public OutParameterGetter<T> getOutParameterGetter() {
        return outParameterGetter;
    }

    public void setOutParameterGetter(OutParameterGetter<T> outParameterGetter) {
        this.outParameterGetter = outParameterGetter;
    }

    public Class getParamGetterClass() {
        return paramGetterClass;
    }

    public void registerParam(CallableStatement callableStatement, int paramIdx) throws SQLException {
        this.registerParam.registerOutParam(paramIdx, callableStatement);
    }

    private interface RegisterParam {
        void registerOutParam(int paramIdx, CallableStatement statement) throws SQLException;
    }
}

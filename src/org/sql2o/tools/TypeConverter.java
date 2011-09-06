package org.sql2o.tools;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 9/6/11
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class TypeConverter {

    public static Object convert(Class type, Object value){

        if (value == null){
            return null;
        }

        // handle jodatime
        if (type.equals(DateTime.class)){
            return new DateTime(value);
        }

        if (type.equals(value.getClass())) {
            return value;
        }


        if (Number.class.isAssignableFrom(type)){
            Number numberVal = (Number)value;
            if (type.equals(Integer.class)){
                return numberVal.intValue();
            }
            else if(type.equals(Long.class)){
                return numberVal.longValue();
            }
            else if(type.equals(Short.class)){
                return numberVal.shortValue();
            }
            else if(type.equals(Double.class)){
                return numberVal.doubleValue();
            }
            else if(type.equals(Float.class)){
                return numberVal.floatValue();
            }
            else if(type.equals(Byte.class)){
                return numberVal.byteValue();
            }
            else{
                throw new RuntimeException("Cannot convert from type " + value.getClass().getName() + " to " + type.getName());
            }
        }

        return value;
    }
}

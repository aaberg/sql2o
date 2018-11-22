package org.sql2o.converters;


import org.junit.Test;


import java.math.BigInteger;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class BigIntegerConverterTest {
    
    private BigIntegerConverter bigIntegerConverter = new BigIntegerConverter();
    
    @Test
    public void convertNumberValue(){
        BigInteger bigInteger = bigIntegerConverter.convertNumberValue(1);
        assertThat(bigInteger,equalTo(BigInteger.ONE));
    }
    
    @Test
    public void convertBigIntegerValue(){
        BigInteger bigInteger = bigIntegerConverter.convertNumberValue(BigInteger.ONE);
        assertThat(bigInteger,equalTo(BigInteger.ONE));
    }
    
    @Test
    public void convertStringValue(){
        BigInteger bigInteger = bigIntegerConverter.convertStringValue("1");
        assertThat(bigInteger,equalTo(BigInteger.ONE));
    }

    @Test
    public void convertNullValue(){
        BigInteger bigInteger;
        
        bigInteger = bigIntegerConverter.convertNumberValue(null);
        assertThat(bigInteger,is(nullValue()));
        
        bigInteger = bigIntegerConverter.convertStringValue(null);
        assertThat(bigInteger,is(nullValue()));

    }
    
    @Test
    public void getTypeDescription(){
        assertThat(bigIntegerConverter.getTypeDescription(),equalTo("class java.math.BigInteger"));
    }
}

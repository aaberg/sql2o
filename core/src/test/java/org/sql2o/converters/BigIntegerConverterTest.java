package org.sql2o.converters;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

public class BigIntegerConverterTest {
    
    private BigIntegerConverter bigIntegerConverter = new BigIntegerConverter();
    
    @Test
    public void convertNumberValue(){
        BigInteger bigInteger = bigIntegerConverter.convertNumberValue(1);
        assertThat(bigInteger).isEqualTo(1);
    }
    
    @Test
    public void convertBigIntegerValue(){
        BigInteger bigInteger = bigIntegerConverter.convertNumberValue(BigInteger.valueOf(1L));
        assertThat(bigInteger).isEqualTo(1);
    }
    
    @Test
    public void convertStringValue(){
        BigInteger bigInteger = bigIntegerConverter.convertStringValue("1");
        assertThat(bigInteger).isEqualTo(1);
    }
    
    @Test
    public void getTypeDescription(){
        assertThat(bigIntegerConverter.getTypeDescription()).isEqualTo("class java.math.BigInteger");
    }
}

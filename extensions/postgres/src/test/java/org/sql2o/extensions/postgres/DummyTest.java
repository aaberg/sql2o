package org.sql2o.extensions.postgres;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 * Created by lars on 03.04.14.
 *
 * Created the class as a placeholder. Should be deleted when actual functionality is added.
 */
public class DummyTest {

    Logger logger = LoggerFactory.getLogger(DummyTest.class);

    @Test
    public void testSomething(){
        String awesome = "awesome";
        String postgres = "awesome";

        assertThat(postgres, is(awesome));
        logger.info("postgres is {}", awesome);
    }
}

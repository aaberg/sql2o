package org.sql2o.converters;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.*;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public class BidirectionalConverterTest {

    private Sql2o sql2o;
    private List<UUIDWrapper> wrappers;

    @Before
    public void setUp()
    {
        Quirks quirks = new NoQuirks(){
            {
                this.converters.put(UUID.class, new CustomUUIDConverter());
            }
        };


        this.sql2o = new Sql2o("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "", quirks);
        this.wrappers = randomWrappers();
        this.createAndFillTable(this.wrappers);
    }

    @After
    public void tearDown()
    {
        deleteTable();
    }

    @Test
    public void toDatabase_fromDatabase_doExecute()
    {
        List<String> notConverted = sql2o.createQuery("select text from uuid_wrapper")
                                         .executeAndFetch(String.class);

        // if conversion to database worked, all "-" from UUID were replaced with "!"
        for (String s : notConverted) {
            assertNotNull(UUID.fromString(s.replace('!', '-')));
        }

        List<UUIDWrapper> converted = sql2o.createQuery("select * from uuid_wrapper")
                                           .executeAndFetch(UUIDWrapper.class);

        // if conversion from database worked, should have the list we inserted
        assertEquals(wrappers, converted);
    }

    /************** Helper stuff ******************/

    private List<UUIDWrapper> randomWrappers() {
        List<UUIDWrapper> wrappers = new ArrayList<UUIDWrapper>();
        for (int i = 0; i < 10; i++) {
            wrappers.add(new UUIDWrapper(UUID.randomUUID()));
        }
        return wrappers;
    }

    private void createAndFillTable(List<UUIDWrapper> wrappers) {
        sql2o.createQuery("create table uuid_wrapper(\n" +
                          "text varchar(100) primary key)").executeUpdate();

        Query insQuery = sql2o.createQuery("insert into uuid_wrapper(text) values (:text)");
        for (UUIDWrapper wrapper : wrappers) {
            insQuery.addParameter("text", wrapper.getText()).addToBatch();
        }
        insQuery.executeBatch();
    }

    private void deleteTable(){
        try {
            sql2o.createQuery("drop table uuid_wrapper").executeUpdate();
        }
        catch(Sql2oException e) {
            // if it fails, its because the User table doesn't exists. Just ignore this.
        }
    }
}
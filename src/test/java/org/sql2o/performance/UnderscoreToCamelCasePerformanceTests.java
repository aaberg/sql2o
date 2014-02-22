package org.sql2o.performance;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sql2o.tools.UnderscoreToCamelCase;

import java.util.List;

/**
 * @author aldenquimby@gmail.com
 */
public class UnderscoreToCamelCasePerformanceTests
{
    private static final int ITERATIONS = 1000;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Test
    public void run()
    {
        PerformanceTestList tests = new PerformanceTestList();
        tests.add(new Sql2oUnderscoreToCamelCase());
        tests.add(new GuavaUnderscoreToCamelCase());

        tests.run(ITERATIONS);
        tests.printResults();
    }

    private List<String> toConvert = ImmutableList.of(
        "my_string_variable", "string", "my_really_long_string_variable_name",
        "my_string2_with_numbers_4", "my_string_with_MixED_CaSe",
        "", " ", "\t", "\n\n"
    );

    //----------------------------------------
    //          performance tests
    // ---------------------------------------

    class Sql2oUnderscoreToCamelCase extends PerformanceTestBase
    {
        @Override
        public void init() {}

        @Override
        public void close() {}

        @Override
        public void run(int input)
        {
            for (String s : toConvert)
            {
                UnderscoreToCamelCase.convert(s);
            }
        }
    }

    class GuavaUnderscoreToCamelCase extends PerformanceTestBase
    {
        @Override
        public void init() {}

        @Override
        public void close() {}

        @Override
        public void run(int input)
        {
            for (String s : toConvert)
            {
                CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, s);
            }
        }
    }
}

package org.sql2o.bytecode.bench;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An employee: eight fields of different kinds — strings, a date, a number, an enum.
 *
 * <p>The fields are public on purpose: a reader cannot be compiled for private ones (which {@code ReaderRefusesTest}
 * pins), and the benchmark compares read speed rather than refusal policies.
 */
public class Employee {

    public String fullName;
    public LocalDate birthDate;
    public BigDecimal salary;
    public Gender gender;
    public String passportSeries;
    public String passportNumber;
    public String email;
    public String phone;

    @Override
    public String toString() {
        return "Employee[" + fullName + ", " + birthDate + ", " + salary + ", " + gender + "]";
    }
}

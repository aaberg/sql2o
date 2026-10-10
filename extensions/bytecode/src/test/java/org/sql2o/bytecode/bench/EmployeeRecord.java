package org.sql2o.bytecode.bench;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The same employee with the same eight fields, but a record rather than a POJO with fields. */
public record EmployeeRecord(
        String fullName,
        LocalDate birthDate,
        BigDecimal salary,
        Gender gender,
        String passportSeries,
        String passportNumber,
        String email,
        String phone) {
}

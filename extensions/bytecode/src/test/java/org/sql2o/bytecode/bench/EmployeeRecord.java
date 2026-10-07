package org.sql2o.bytecode.bench;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Тот же сотрудник теми же восемью полями, но record вместо POJO с полями. */
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

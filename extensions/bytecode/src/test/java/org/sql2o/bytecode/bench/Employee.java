package org.sql2o.bytecode.bench;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Сотрудник: восемь разнотипных полей — строки, дата, число, enum.
 *
 * <p>Поля публичные сознательно: приватные ридер скомпилировать не может (и это проверено
 * {@code ReaderRefusesTest}), а бенчмарк сравнивает скорость чтения, а не политики отказа.
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

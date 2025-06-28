package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa un empleado de la organización.
 * <p>
 * Incluye información de identificación, rol, departamento,
 * fecha de contratación, salario y trazabilidad de creación/actualización.
 * </p>
 *
 * @author Silver VS
 * @version 1.0
 * @since 2025-06-27
 */
public class Employee {

    /** Identificador único del empleado (clave primaria). */
    private int id;

    /** Nombre completo del empleado. */
    private String name;

    /** Puesto o rol que desempeña el empleado. */
    private String position;

    /** Departamento al que pertenece el empleado. */
    private String department;

    /** Fecha en que fue contratado el empleado. */
    private LocalDate hire_date;

    /** Salario actual del empleado. */
    private BigDecimal salary;

    /**
     * Constructor vacío (requerido por algunos frameworks de serialización).
     */
    public Employee() {
        // Intentionally empty.
    }

    /**
     * Constructor para creación de un nuevo empleado.
     *
     * @param name       Nombre completo
     * @param position   Puesto o rol
     * @param salary     Salario inicial
     * @param hire_date   Fecha de contratación
     * @param department Departamento asignado
     */
    public Employee(String name,
                    String position,
                    BigDecimal salary,
                    LocalDate hire_date,
                    String department
    ) {
        this.name       = name;
        this.position   = position;
        this.salary     = salary;
        this.hire_date = hire_date;
        this.department = department;
    }

    /**
     * Constructor completo con todos los campos.
     *
     * @param id          Identificador único
     * @param name        Nombre completo
     * @param position    Puesto o rol
     * @param salary      Salario actual
     * @param hire_date    Fecha de contratación
     * @param department  Departamento asignado
     */
    public Employee(int id,
                    String name,
                    String position,
                    BigDecimal salary,
                    LocalDate hire_date,
                    String department) {
        this.id          = id;
        this.name        = name;
        this.position    = position;
        this.department  = department;
        this.hire_date = hire_date;
        this.salary      = salary;
    }

    /** @return identificador único del empleado */
    public int getId() {
        return id;
    }

    /** @param id establece el identificador único del empleado */
    public void setId(int id) {
        this.id = id;
    }

    /** @return nombre completo del empleado */
    public String getName() {
        return name;
    }

    /** @param name establece el nombre completo del empleado */
    public void setName(String name) {
        this.name = name;
    }

    /** @return puesto o rol del empleado */
    public String getPosition() {
        return position;
    }

    /** @param position establece el puesto o rol del empleado */
    public void setPosition(String position) {
        this.position = position;
    }

    /** @return departamento al que pertenece el empleado */
    public String getDepartment() {
        return department;
    }

    /** @param department establece el departamento del empleado */
    public void setDepartment(String department) {
        this.department = department;
    }

    /** @return fecha de contratación */
    public LocalDate getHire_date() {
        return hire_date;
    }

    /** @param hire_date establece la fecha de contratación */
    public void setHire_date(LocalDate hire_date) {
        this.hire_date = hire_date;
    }

    /** @return salario actual del empleado */
    public BigDecimal getSalary() {
        return salary;
    }

    /** @param salary establece el salario del empleado */
    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }


    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", position='" + position + '\'' +
                ", department='" + department + '\'' +
                ", hireDate=" + hire_date +
                ", salary=" + salary +
                '}';
    }
}

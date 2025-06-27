/*
 * Employee.java
 * ---------------------------------------------------------------------------
 * Copyright © 2025 Silver VS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 * ---------------------------------------------------------------------------
 */

package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Representa un empleado con sus datos básicos y timestamps de auditoría.
 *
 * @author Silver VS
 * @version 1.0
 * @since 2025-06-26
 */
public class Employee {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private BigDecimal salary;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Constructor vacío para uso de frameworks y DAO.
     */
    public Employee() {
    }

    /**
     * Constructor para creación de nuevo empleado.
     *
     * @param firstName nombre del empleado
     * @param lastName  apellido del empleado
     * @param email     correo electrónico
     * @param salary    salario del empleado
     */
    public Employee(String firstName, String lastName, String email, BigDecimal salary) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.salary = salary;
    }

    /**
     * Obtiene el ID del empleado.
     *
     * @return identificador único asignado por la base de datos
     */
    public int getId() {
        return id;
    }

    /**
     * Asigna el ID del empleado.
     *
     * @param id identificador único
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre del empleado.
     *
     * @return nombre propio
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Asigna el nombre del empleado.
     *
     * @param firstName nombre propio
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Obtiene el apellido del empleado.
     *
     * @return apellido paterno o materno
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Asigna el apellido del empleado.
     *
     * @param lastName apellido paterno o materno
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Obtiene el correo electrónico del empleado.
     *
     * @return email único para contacto
     */
    public String getEmail() {
        return email;
    }

    /**
     * Asigna el correo electrónico del empleado.
     *
     * @param email dirección de correo única
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Obtiene el salario del empleado.
     *
     * @return salario en formato decimal
     */
    public BigDecimal getSalary() {
        return salary;
    }

    /**
     * Asigna el salario del empleado.
     *
     * @param salary salario en formato decimal
     */
    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    /**
     * Obtiene la fecha de creación del registro.
     *
     * @return timestamp de creación
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * Asigna la fecha de creación del registro.
     *
     * @param createdAt timestamp de creación
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Obtiene la fecha de la última actualización del registro.
     *
     * @return timestamp de última modificación
     */
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Asigna la fecha de la última actualización del registro.
     *
     * @param updatedAt timestamp de última modificación
     */
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

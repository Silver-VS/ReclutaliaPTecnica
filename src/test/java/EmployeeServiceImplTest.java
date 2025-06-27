/*
 * EmployeeServiceImplTest.java
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

import model.Employee;
import model.EmployeeDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.EmployeeServiceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link service.EmployeeServiceImpl} usando Mockito para simular el DAO.
 * <p>
 * Verifica validaciones y delegación de operaciones CRUD.
 * También prueba el manejo de errores en top10Salaries().
 * </p>
 *
 * @author Silver VS
 * @version 1.0
 * @since 2025-06-27
 */
@ExtendWith(MockitoExtension.class)
public class EmployeeServiceImplTest {

    @Mock
    private EmployeeDAO dao;

    @InjectMocks
    private EmployeeServiceImpl service;

    private Employee validEmployee;

    @BeforeEach
    public void setup() {
        validEmployee = new Employee("Alice", "Wonderland", "alice@example.com", new BigDecimal("6000.00"));
        validEmployee.setId(1);
    }

    @Test
    public void create_ValidEmployee_ReturnsId() {
        when(dao.createEmployee(validEmployee)).thenReturn(42);

        int id = service.create(validEmployee);

        assertEquals(42, id);
        verify(dao).createEmployee(validEmployee);
    }

    @Test
    public void create_InvalidEmployee_ThrowsException() {
        Employee e = new Employee(null, "Last", "email@x.com", new BigDecimal("1000"));
        assertThrows(IllegalArgumentException.class, () -> service.create(e));
        verifyNoInteractions(dao);
    }

    @Test
    public void findById_InvalidId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.findById(0));
        verifyNoInteractions(dao);
    }

    @Test
    public void findById_ExistingId_ReturnsEmployee() {
        when(dao.getEmployeeById(1)).thenReturn(Optional.of(validEmployee));

        Optional<Employee> result = service.findById(1);

        assertTrue(result.isPresent());
        assertEquals(validEmployee, result.get());
    }

    @Test
    public void findAll_ReturnsList() {
        List<Employee> list = Collections.singletonList(validEmployee);
        when(dao.getAllEmployees()).thenReturn(list);

        List<Employee> result = service.findAll();

        assertEquals(list, result);
        verify(dao).getAllEmployees();
    }

    @Test
    public void update_InvalidId_ThrowsException() {
        Employee e = new Employee("A", "B", "a@b.com", new BigDecimal("1000"));
        e.setId(0);
        assertThrows(IllegalArgumentException.class, () -> service.update(e));
        verifyNoInteractions(dao);
    }

    @Test
    public void update_Valid_DelegatesToDao() {
        when(dao.updateEmployee(validEmployee)).thenReturn(true);
        boolean result = service.update(validEmployee);
        assertTrue(result);
        verify(dao).updateEmployee(validEmployee);
    }

    @Test
    public void delete_InvalidId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.delete(0));
        verifyNoInteractions(dao);
    }

    @Test
    public void delete_Valid_DelegatesToDao() {
        when(dao.deleteEmployee(1)).thenReturn(true);
        boolean result = service.delete(1);
        assertTrue(result);
        verify(dao).deleteEmployee(1);
    }

    @Test
    public void top10Salaries_MissingResources_Throws() {
        // Suponiendo que no hay recursos JSON en classpath
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.top10Salaries());
        assertTrue(ex.getMessage().contains("Error al procesar salarios"));
    }
}

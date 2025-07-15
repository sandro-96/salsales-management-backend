// File: test/java/com/example/sales/service/CustomerServiceTest.java
package com.example.sales.service;

import com.example.sales.dto.customer.CustomerRequest;
import com.example.sales.dto.customer.CustomerResponse;
import com.example.sales.model.Customer;
import com.example.sales.model.User;
import com.example.sales.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @InjectMocks
    private CustomerService customerService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditLogService auditLogService;

    @Test
    void testCreateCustomer() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Nguyễn Văn A");

        User user = new User();
        user.setId("user1");

        when(customerRepository.save(any())).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId("c1");
            return customer;
        });

        CustomerResponse response = customerService.createCustomer("shop1", user, request);

        assertEquals("Nguyễn Văn A", response.getName());
        verify(customerRepository).save(any(Customer.class));
        verify(auditLogService).log(any(), any(), any(), any(), any(), any());
    }
}
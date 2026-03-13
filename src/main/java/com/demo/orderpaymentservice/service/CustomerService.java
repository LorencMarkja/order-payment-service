package com.demo.orderpaymentservice.service;

import com.demo.orderpaymentservice.domain.Customer;
import com.demo.orderpaymentservice.dto.request.CustomerRequest;
import com.demo.orderpaymentservice.dto.response.CustomerResponse;
import com.demo.orderpaymentservice.exception.ConflictException;
import com.demo.orderpaymentservice.exception.NotFoundException;
import com.demo.orderpaymentservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }

        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setEmail(request.email());

        Customer customerSaved = customerRepository.save(customer);

        return toResponse(customerSaved);
    }

    public CustomerResponse getCustomer(Long id) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Customer not found: " + id));
        return toResponse(customer);
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail());
    }
}
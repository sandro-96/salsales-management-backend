package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.dto.CustomerSearchRequest;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.helper.CustomerSearchHelper;
import com.example.sales.model.Customer;
import com.example.sales.model.User;
import com.example.sales.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final MongoTemplate mongoTemplate;

    private final CustomerRepository customerRepository;

    private final CustomerSearchHelper searchHelper;

    private final ExcelExportService excelExportService;

    public List<Customer> getAllByUser(User user) {
        return customerRepository.findByUserId(user.getId());
    }

    public Customer createCustomer(User user, Customer customer) {
        customer.setId(null);
        customer.setUserId(user.getId());
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(User user, String id, Customer updated) {
        Customer existing = customerRepository.findById(id)
                .filter(c -> c.getUserId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CUSTOMER_NOT_FOUND));

        existing.setName(updated.getName());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());
        existing.setAddress(updated.getAddress());
        existing.setNote(updated.getNote());

        return customerRepository.save(existing);
    }

    public void deleteCustomer(User user, String id) {
        Customer existing = customerRepository.findById(id)
                .filter(c -> c.getUserId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CUSTOMER_NOT_FOUND));

        customerRepository.delete(existing);
    }

    public Page<Customer> searchWithAggregation(User user, CustomerSearchRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());
        List<Customer> content = searchHelper.search(user.getId(), req, pageable);
        long total = searchHelper.counts(user.getId(), req);
        return new PageImpl<>(content, pageable, total);
    }

    public List<Customer> exportAllCustomers(User user, CustomerSearchRequest req) {
        return searchHelper.exportAll(user.getId(), req);
    }

    public void softDeleteCustomer(User user, String id) {
        Customer customer = customerRepository.findById(id)
                .filter(c -> c.getUserId().equals(user.getId()) && !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CUSTOMER_NOT_FOUND));

        customer.setDeleted(true);
        customer.setDeletedAt(LocalDateTime.now());

        customerRepository.save(customer);
    }

    public ResponseEntity<byte[]> exportCustomersExcel(User user) {
        List<Customer> customers = getCustomersByUser(user); // đã có filter deleted nếu cần

        List<String> headers = List.of("ID", "Tên", "Email", "SĐT", "Ngày tạo");

        Function<Customer, List<String>> mapper = c -> List.of(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : ""
        );

        return excelExportService.exportExcel("customers.xlsx", "Customers", headers, customers, mapper);
    }

    public List<Customer> getCustomersByUser(User user) {
        return customerRepository.findByUserIdAndDeletedFalse(user.getId());
    }

    public List<Customer> searchCustomers(User user, CustomerSearchRequest req) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where("userId").is(user.getId()));
        criteriaList.add(Criteria.where("deleted").ne(true));

        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            Criteria keyword = new Criteria().orOperator(
                    Criteria.where("name").regex(req.getKeyword(), "i"),
                    Criteria.where("email").regex(req.getKeyword(), "i"),
                    Criteria.where("phone").regex(req.getKeyword(), "i")
            );
            criteriaList.add(keyword);
        }

        if (req.getFromDate() != null) {
            criteriaList.add(Criteria.where("createdAt").gte(req.getFromDate().atStartOfDay()));
        }
        if (req.getToDate() != null) {
            criteriaList.add(Criteria.where("createdAt").lt(req.getToDate().plusDays(1).atStartOfDay()));
        }

        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        return mongoTemplate.find(query, Customer.class);
    }

    public ResponseEntity<byte[]> exportCustomers(User user, CustomerSearchRequest req) {
        List<Customer> customers = searchCustomers(user, req);

        List<String> headers = List.of("ID", "Tên", "Email", "SĐT", "Ngày tạo");

        Function<Customer, List<String>> mapper = c -> List.of(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : ""
        );

        return excelExportService.exportExcel("customers.xlsx", "Customers", headers, customers, mapper);
    }

    public Page<Customer> searchCustomersPaged(User user, CustomerSearchRequest req) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where("userId").is(user.getId()));
        criteriaList.add(Criteria.where("deleted").ne(true));

        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            Criteria keyword = new Criteria().orOperator(
                    Criteria.where("name").regex(req.getKeyword(), "i"),
                    Criteria.where("email").regex(req.getKeyword(), "i"),
                    Criteria.where("phone").regex(req.getKeyword(), "i")
            );
            criteriaList.add(keyword);
        }

        if (req.getFromDate() != null) {
            criteriaList.add(Criteria.where("createdAt").gte(req.getFromDate().atStartOfDay()));
        }
        if (req.getToDate() != null) {
            criteriaList.add(Criteria.where("createdAt").lt(req.getToDate().plusDays(1).atStartOfDay()));
        }

        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));

        String sortBy = (req.getSortBy() == null || req.getSortBy().isBlank()) ? "createdAt" : req.getSortBy();
        String sortDir = (req.getSortDir() == null || req.getSortDir().isBlank()) ? "desc" : req.getSortDir();

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        query.with(Sort.by(direction, sortBy));

        // Phân trang
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());
        query.with(pageable);

        long total = mongoTemplate.count(query, Customer.class);
        List<Customer> customers = mongoTemplate.find(query, Customer.class);

        return new PageImpl<>(customers, pageable, total);
    }
}

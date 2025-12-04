package com.example.backend.controller;

import com.example.backend.dto.OrderRequest;
import com.example.backend.dto.OrderStatus;
import com.example.backend.entity.Order;
import com.example.backend.entity.User;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;

    // CREATE ORDER
    @PostMapping("/create")
    public Order create(@RequestBody OrderRequest req) {

        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setAddress(req.getAddress());
        order.setTotalAmount(req.getTotalAmount());
        order.setStatus(OrderStatus.valueOf("PENDING"));

        return orderRepo.save(order);
    }

    // GET ALL ORDERS
    @GetMapping
    public List<Order> getAll() {
        return orderRepo.findAll();
    }

    // GET ONE ORDER
    @GetMapping("/{id}")
    public Order getOne(@PathVariable Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    // UPDATE ORDER STATUS
    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable Long id, @RequestBody String status) {

        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.valueOf(status.replace("\"", ""))); // JSON string bo‘lganda qo‘shtirnoqni olib tashlash
        return orderRepo.save(order);
    }

    // DELETE ORDER
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {

        orderRepo.deleteById(id);
        return "Order deleted";
    }
}

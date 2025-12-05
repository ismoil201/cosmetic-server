package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by id
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Create user
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // Update user
    public User updateUser(Long id, User newUser) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setEmail(newUser.getEmail());
                    existingUser.setFullName(newUser.getFullName());
                    existingUser.setPassword(newUser.getPassword());
                    return userRepository.save(existingUser);
                })
                .orElse(null);
    }

    // Delete user
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

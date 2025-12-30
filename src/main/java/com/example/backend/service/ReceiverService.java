package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.ReceiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiverService {

    private final ReceiverRepository receiverRepo;
    private final UserService userService;

    public ReceiverResponse create(ReceiverCreateRequest req) {

        User user = userService.getCurrentUser();

        Receiver receiver = new Receiver();
        receiver.setUser(user);
        receiver.setFirstName(req.getFirstName());
        receiver.setLastName(req.getLastName());
        receiver.setPhone(req.getPhone());

        receiver = receiverRepo.save(receiver);

        return map(receiver);
    }

    public List<ReceiverResponse> myReceivers() {
        User user = userService.getCurrentUser();

        return receiverRepo.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    public Receiver getById(Long id) {
        return receiverRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
    }

    private ReceiverResponse map(Receiver r) {
        return new ReceiverResponse(
                r.getId(),
                r.getFirstName() + " " + r.getLastName(),
                r.getPhone()
        );
    }

    public Receiver getOwnedByCurrentUser(Long receiverId) {
        User user = userService.getCurrentUser();
        Receiver r = receiverRepo.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (!r.isActive() || !r.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Receiver not allowed");
        }
        return r;
    }

}

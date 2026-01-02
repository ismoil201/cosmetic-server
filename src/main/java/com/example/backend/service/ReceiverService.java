package com.example.backend.service;

import com.example.backend.dto.ReceiverCreateRequest;
import com.example.backend.dto.ReceiverResponse;
import com.example.backend.entity.Receiver;
import com.example.backend.entity.User;
import com.example.backend.repository.ReceiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public void delete(Long receiverId) {
        User user = userService.getCurrentUser();

        Receiver r = receiverRepo.findById(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver not found"));

        boolean active = Boolean.TRUE.equals(r.isActive()); // agar Boolean bo‘lsa
        // agar entity’da primitive boolean bo‘lsa: boolean active = r.isActive();

        if (!active || !r.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Receiver not allowed");
        }

        r.setActive(false);
        receiverRepo.save(r);
    }
    public Receiver getOwnedByCurrentUser(Long receiverId) {

        User user = userService.getCurrentUser();

        Receiver r = receiverRepo.findById(receiverId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Receiver not found"
                        )
                );

        boolean active = Boolean.TRUE.equals(r.isActive());
        // agar primitive boolean bo‘lsa → r.isActive()

        if (!active || !r.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Receiver not allowed"
            );
        }

        return r;
    }

    private ReceiverResponse map(Receiver r) {
        return new ReceiverResponse(
                r.getId(),
                r.getFirstName() + " " + r.getLastName(),
                r.getPhone()
        );
    }
}

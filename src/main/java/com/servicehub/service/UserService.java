package com.servicehub.service;

import com.servicehub.dto.UserRequest;
import com.servicehub.dto.UserResponse;
import com.servicehub.exception.DuplicateResourceException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.User;
import com.servicehub.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Já existe um usuário com este e-mail");
        }

        User user = new User();
        copyRequest(user, request);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(findEntity(id));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = findEntity(id);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Já existe outro usuário com este e-mail");
        }
        copyRequest(user, request);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(findEntity(id));
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }

    private void copyRequest(User user, UserRequest request) {
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        user.setUserType(request.userType());
    }

}

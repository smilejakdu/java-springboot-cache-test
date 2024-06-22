package com.fastcampus.cache.controller;

import com.fastcampus.cache.domain.entity.User;
import com.fastcampus.cache.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/user{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }
}
package com.project.ewallet.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.ewallet.model.User;
import com.project.ewallet.request.UserCreateRequest;
import com.project.ewallet.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {
    @Autowired
    UserService userService;

    @PostMapping("/user")
    public void createUser(@RequestBody UserCreateRequest userCreateRequest) throws JsonProcessingException {
        userService.create(userCreateRequest);
    }

    @GetMapping("/user")
    public User getUserDetails(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User)authentication.getPrincipal();
        return userService.loadUserByUsername(user.getUsername());
    }

    @GetMapping("/admin/all/users")
    public List<User> getAllUserDetails(){
        return userService.getAll();
    }

    @GetMapping("/admin/user/{userId}")
    public User getUserDetails(@PathVariable("userId") String userId){

        return userService.loadUserByUsername(userId);
    }
}

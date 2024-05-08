package com.project.ewallet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ewallet.CommonConstants;
import com.project.ewallet.configuration.CacheConfig;
import com.project.ewallet.constants.UserConstants;
import com.project.ewallet.model.User;
import com.project.ewallet.repository.CacheRepository;
import com.project.ewallet.repository.UserRepository;
import com.project.ewallet.request.UserCreateRequest;
import org.apache.kafka.common.protocol.types.Field;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CacheRepository cacheRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public User loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        User user = cacheRepository.get(phoneNumber);
        if(user==null){
            user = userRepository.findByPhoneNumber(phoneNumber);
            if(user!=null)
                cacheRepository.set(user);
        }

        return user;
    }


    public void create(UserCreateRequest userCreateRequest) throws JsonProcessingException {
        User user = userCreateRequest.to();
        user.setPassword(encryptPassword(user.getPassword()));
        user.setAuthorities(UserConstants.USER_AUTHORITY);

        userRepository.save(user);

        //TODO: publish the event post user creation which can be listened by consumers.
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(CommonConstants.USER_CREATION_TOPIC_USERID, user.getId());
        jsonObject.put(CommonConstants.USER_CREATION_TOPIC_PHONE_NUMBER, user.getPhoneNumber());
        jsonObject.put(CommonConstants.USER_CREATION_TOPIC_IDENTIFIER_KEY, user.getUserIdentifier());
        jsonObject.put(CommonConstants.USER_CREATION_TOPIC_IDENTIFIER_VALUE, user.getIdentifierValue());
        kafkaTemplate.send(CommonConstants.USER_CREATION_TOPIC, objectMapper.writeValueAsString(jsonObject));

    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public String encryptPassword(String rawPassword){
        return passwordEncoder.encode(rawPassword);
    }
}

package com.project.ewallet.repository;

import com.project.ewallet.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class CacheRepository {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private final String USER_KEY_PREFIX = "user::";

    /**
     * username- ashish
     * key - user::ashish
     */
    public void set(User User){
        String key = getKey(User.getUsername());
        redisTemplate.opsForValue().set(key, User, 24, TimeUnit.HOURS);
    }

    public User get(String username){
        String key = getKey(username);
        return (User)redisTemplate.opsForValue().get(key);
    }

    public String getKey(String username){
        return USER_KEY_PREFIX+username;
    }
}

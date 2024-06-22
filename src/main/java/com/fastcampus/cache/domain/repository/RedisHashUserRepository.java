package com.fastcampus.cache.domain.repository;

import com.fastcampus.cache.domain.entity.RedisHashUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisHashUserRepository extends CrudRepository<RedisHashUser, Long> {
}

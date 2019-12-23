package com.sberbank.sweater.Repo;

import com.sberbank.sweater.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findByUsername(String username);

    User findByActivationCode(String code);

}

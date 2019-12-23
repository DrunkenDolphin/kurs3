package com.sberbank.sweater.Repo;

import com.sberbank.sweater.Entities.Message;
import com.sberbank.sweater.Entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends CrudRepository<Message, Integer> {

    Page<Message> findByTag(String tag, Pageable pageable);
    Page<Message> findAll(Pageable pageable);

    @Query("from Message as m where m.author = :author")
    Page<Message> findByUser(Pageable pageable, @Param("author") User user);
}

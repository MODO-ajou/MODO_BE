package com.example.modo_be.user.repository;

import com.example.modo_be.book.domain.Book;
import com.example.modo_be.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUserEmail(String userEmail);
    User findByUserEmail(String userEmail);

}

package ru.practicum.user.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users", schema = "public")
@Data
public class User {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;
}
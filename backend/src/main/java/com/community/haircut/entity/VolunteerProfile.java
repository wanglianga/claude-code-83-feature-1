package com.community.haircut.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "volunteer_profile")
public class VolunteerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Integer serviceCount = 0;

    @Column(nullable = false)
    private Boolean active = true;
}

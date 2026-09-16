package com.community.haircut.repository;

import com.community.haircut.entity.BarberSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BarberScheduleRepository extends JpaRepository<BarberSchedule, Long> {

    List<BarberSchedule> findByWorkDateAndStatus(LocalDate workDate, String status);

    List<BarberSchedule> findByBarberIdAndWorkDateBetween(Long barberId, LocalDate from, LocalDate to);

    List<BarberSchedule> findByWorkDateBetween(LocalDate from, LocalDate to);
}

package com.community.haircut.repository;

import com.community.haircut.entity.ServiceOrder;
import com.community.haircut.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    boolean existsByElderIdAndScheduledDateAndStatusNotIn(Long elderId, LocalDate date, Collection<OrderStatus> statuses);

    long countByBarberIdAndScheduledDateAndStatusNotIn(Long barberId, LocalDate date, Collection<OrderStatus> statuses);

    long countByVolunteerIdAndScheduledDateAndStatusNotIn(Long volunteerId, LocalDate date, Collection<OrderStatus> statuses);

    List<ServiceOrder> findByElderIdOrderByCreatedAtDesc(Long elderId);

    List<ServiceOrder> findByBarberIdOrderByScheduledDateDesc(Long barberId);

    List<ServiceOrder> findByVolunteerIdOrderByScheduledDateDesc(Long volunteerId);

    List<ServiceOrder> findByScheduledDate(LocalDate date);

    List<ServiceOrder> findByStatus(OrderStatus status);

    List<ServiceOrder> findByEventId(Long eventId);

    List<ServiceOrder> findByScheduledDateBetween(LocalDate from, LocalDate to);

    List<ServiceOrder> findAllByOrderByCreatedAtDesc();
}

package com.ivoyant.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long vehicleId;
    private Long slotId;
    private String serviceType;
    private String status; // PENDING, IN_PROGRESS, COMPLETED
    private Double amount;
    private LocalDateTime bookingDate;
    private Long paymentId;
}


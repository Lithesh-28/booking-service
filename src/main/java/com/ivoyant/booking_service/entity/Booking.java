package com.ivoyant.booking_service.entity;

import com.ivoyant.booking_service.contant.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.*;

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

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Vehicle ID cannot be null")
    private Long vehicleId;

    private Long slotId;

    @NotBlank(message = "Service type cannot be blank")
    private String serviceType;

    @NotNull(message = "Booking status cannot be null")
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    private LocalDateTime bookingDate;
    private Long paymentId;
}

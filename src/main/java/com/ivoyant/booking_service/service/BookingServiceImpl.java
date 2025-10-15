package com.ivoyant.booking_service.service;

import com.ivoyant.booking_service.dto.PaymentRequest;
import com.ivoyant.booking_service.dto.PaymentResponse;
import com.ivoyant.booking_service.entity.Booking;
import com.ivoyant.booking_service.exception.BookingNotFoundException;
import com.ivoyant.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;

    @Override
    public Booking createBooking(Booking booking) {
        log.info("Starting booking for vehicle ID: {}", booking.getVehicleId());

        // 1️ Check vehicle
        String vehicleUrl = "http://localhost:8081/vehicles/" + booking.getVehicleId();
        Object vehicle = restTemplate.getForObject(vehicleUrl, Object.class);
        if (vehicle == null) {
            throw new RuntimeException("Vehicle not found!");
        }

        // 2️ Allocate slot from Workshop Service
        String slotUrl = "http://localhost:8082/workshop/slots";
        Object[] slots = restTemplate.getForObject(slotUrl, Object[].class);
        if (slots == null || slots.length == 0) {
            throw new RuntimeException("No available slots!");
        }

        Map<String, Object> slot = (Map<String, Object>) slots[0];
        Long allocatedSlotId = Long.parseLong(slot.get("id").toString());
        booking.setSlotId(allocatedSlotId);

        // 3️ Set initial details
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus("PENDING");

        // 4️ Save booking
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking saved with ID: {}", savedBooking.getId());

        // 5️ Process payment
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setBookingId(savedBooking.getId());
        paymentRequest.setAmount(savedBooking.getAmount());

        PaymentResponse paymentResponse = restTemplate.postForObject(
                "http://localhost:8083/payments",
                paymentRequest,
                PaymentResponse.class
        );

        // 6️ Update booking after successful payment
        if (paymentResponse != null && "SUCCESS".equals(paymentResponse.getStatus())) {
            savedBooking.setStatus("CONFIRMED");
            savedBooking.setPaymentId(paymentResponse.getId());
            log.info("Payment processed successfully with ID: {}", paymentResponse.getId());
        }

        // 7️ Save final state
        return bookingRepository.save(savedBooking);
    }


    @Override
    public Booking getBookingById(Long id) {
        log.info("Fetching booking ID: {}", id);
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));
    }

    @Override
    public List<Booking> getAllBookings() {
        log.info("Fetching all bookings");
        return bookingRepository.findAll();
    }

    @Override
    public Booking updateBookingStatus(Long id, String status) {
        log.info("Updating booking ID {} status to {}", id, status);
        Booking booking = getBookingById(id);
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    @Override
    public void deleteBooking(Long id) {
        log.info("Deleting booking ID: {}", id);
        Booking booking = getBookingById(id);
        bookingRepository.delete(booking);
    }
}


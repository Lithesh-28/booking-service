package com.ivoyant.booking_service.service;

import com.ivoyant.booking_service.dto.PaymentRequest;
import com.ivoyant.booking_service.dto.PaymentResponse;
import com.ivoyant.booking_service.entity.Booking;
import com.ivoyant.booking_service.exception.BookingNotFoundException;
import com.ivoyant.booking_service.exception.SlotNotFoundException;
import com.ivoyant.booking_service.exception.VehicleNotFoundException;
import com.ivoyant.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${booking-service.vehicle-service-name}")
    private String vehicleServiceName;

    @Value("${booking-service.workshop-service-name}")
    private String workshopServiceName;

    @Value("${booking-service.payment-service-name}")
    private String paymentServiceName;

    @Override
    public Booking createBooking(Booking booking) {
        log.info("Starting booking for vehicle ID: {}", booking.getVehicleId());

        // Check vehicle via Vehicle Service
        String vehicleUrl = "http://" + vehicleServiceName + "/vehicles/" + booking.getVehicleId();
        Object vehicle = restTemplate.getForObject(vehicleUrl, Object.class);
        if (vehicle == null) {
            throw new VehicleNotFoundException("Vehicle not found for the specified vehicle id!");
        }
        log.info("Vehicle exists: {}", booking.getVehicleId());

        // Allocate slot from Workshop Service
        String slotUrl = "http://" + workshopServiceName + "/workshop/slots";
        Object[] slots = restTemplate.getForObject(slotUrl, Object[].class);
        if (slots == null || slots.length == 0) {
            throw new SlotNotFoundException("No available slots Currently choose different time!");
        }

        Map<String, Object> slot = (Map<String, Object>) slots[0]; // pick first slot
        Long allocatedSlotId = Long.parseLong(slot.get("id").toString());
        booking.setSlotId(allocatedSlotId);
        log.info("Slot allocated: {}", allocatedSlotId);

        // Set initial booking details
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus("PENDING");

        // Save booking to get an ID
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking saved with ID: {}", savedBooking.getId());

        // Process payment via Payment Service
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setBookingId(savedBooking.getId());
        paymentRequest.setAmount(savedBooking.getAmount());

        String paymentUrl = "http://" + paymentServiceName + "/payments";
        PaymentResponse paymentResponse = restTemplate.postForObject(paymentUrl, paymentRequest, PaymentResponse.class);

        // Update booking after successful payment
        if (paymentResponse != null && "SUCCESS".equals(paymentResponse.getStatus())) {
            savedBooking.setStatus("CONFIRMED");
            savedBooking.setPaymentId(paymentResponse.getId());
            log.info("Payment processed successfully with ID: {}", paymentResponse.getId());
        }

        // Save final state
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

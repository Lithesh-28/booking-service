package com.ivoyant.booking_service.service;

import com.ivoyant.booking_service.contant.BookingStatus;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
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
        log.info("Starting booking creation for vehicle ID: {}", booking.getVehicleId());

        //1. Validate Vehicle existence
        try {
            String vehicleUrl = "http://" + vehicleServiceName + "/vehicles/" + booking.getVehicleId();
            restTemplate.getForObject(vehicleUrl, Object.class);
            log.info("Vehicle validated successfully for vehicleId: {}", booking.getVehicleId());
        } catch (HttpClientErrorException.NotFound e) {
            throw new VehicleNotFoundException("Vehicle not found for ID: " + booking.getVehicleId());
        } catch (ResourceAccessException e) {
            throw new RuntimeException(" Vehicle Service is currently unavailable!");
        }

        // 2. Get available slots from Workshop Service
        Object[] slots;
        try {
            String slotUrl = "http://" + workshopServiceName + "/workshop/slots";
            slots = restTemplate.getForObject(slotUrl, Object[].class);
        } catch (Exception e) {
            throw new SlotNotFoundException("Unable to fetch slots from Workshop Service!");
        }

        if (slots == null || slots.length == 0) {
            throw new SlotNotFoundException("No available slots! Please choose a different time.");
        }

        Map<String, Object> slot = (Map<String, Object>) slots[0]; // pick first slot
        Long allocatedSlotId = Long.parseLong(slot.get("id").toString());
        booking.setSlotId(allocatedSlotId);
        log.info("Slot allocated successfully with ID: {}", allocatedSlotId);

        // 3. Set booking details
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);

        // 4. Save booking before payment
        Booking savedBooking = bookingRepository.save(booking);
        log.info(" Booking saved in DB with ID: {}", savedBooking.getId());

        // 5. Process Payment via Payment Service
        PaymentRequest paymentRequest = new PaymentRequest(savedBooking.getAmount(), savedBooking.getId());
        PaymentResponse paymentResponse;

        try {
            String paymentUrl = "http://" + paymentServiceName + "/payments";
            log.info("Calling Payment Service at URL: {}", paymentUrl);
            paymentResponse = restTemplate.postForObject(paymentUrl, paymentRequest, PaymentResponse.class);
            log.info("Payment Service Response: {}", paymentResponse);
        } catch (Exception e) {
            log.error(" Payment service call failed: {}", e.getMessage());
            savedBooking.setStatus(BookingStatus.FAILED);
            return bookingRepository.save(savedBooking);
        }

        //  6. Handle Payment Response
        if (paymentResponse == null) {
            log.error(" Payment response was null for booking ID: {}", savedBooking.getId());
            savedBooking.setStatus(BookingStatus.FAILED);
        } else if ("SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
            savedBooking.setStatus(BookingStatus.CONFIRMED);
            savedBooking.setPaymentId(paymentResponse.getId());
            log.info(" Payment successful for booking ID: {}, payment ID: {}", savedBooking.getId(), paymentResponse.getId());
        } else {
            savedBooking.setStatus(BookingStatus.FAILED);
            log.warn(" Payment failed for booking ID: {}", savedBooking.getId());
        }

        //  7. Save final booking state
        Booking finalBooking = bookingRepository.save(savedBooking);
        log.info("Booking process completed successfully for booking ID: {}", finalBooking.getId());
        return finalBooking;
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

        try {
            BookingStatus newStatus = BookingStatus.valueOf(status.toUpperCase());
            booking.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            log.error("Invalid booking status value: {}", status);
            throw new IllegalArgumentException("Invalid booking status: " + status);
        }

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking status updated successfully for ID: {}", id);
        return updatedBooking;
    }


    @Override
    public void deleteBooking(Long id) {
        log.info("Deleting booking ID: {}", id);
        Booking booking = getBookingById(id);
        bookingRepository.delete(booking);
        log.info(" Booking deleted successfully for ID: {}", id);
    }
}

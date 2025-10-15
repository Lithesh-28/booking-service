package com.ivoyant.booking_service.service;


import com.ivoyant.booking_service.entity.Booking;

import java.util.List;

public interface BookingService {
    Booking createBooking(Booking booking);
    Booking getBookingById(Long id);
    List<Booking> getAllBookings();
    Booking updateBookingStatus(Long id, String status);
    void deleteBooking(Long id);
}

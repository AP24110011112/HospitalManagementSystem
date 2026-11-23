package hospital.service;

import java.time.LocalDate;
import java.time.LocalTime;

public final class Validation {
    private Validation() { }
    public static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }
    public static String email(String value) {
        String email = required(value, "Email");
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            throw new IllegalArgumentException("Enter a valid email address.");
        return email;
    }
    public static void password(char[] password) {
        if (password == null || password.length < 8)
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        boolean digit = false;
        for (char c : password) digit |= Character.isDigit(c);
        if (!digit) throw new IllegalArgumentException("Password must include at least one number.");
    }
    public static String phone(String value) {
        if (value == null || value.isBlank()) return "";
        String phone = value.trim();
        if (!phone.matches("[+0-9() -]{7,20}")) throw new IllegalArgumentException("Enter a valid phone number.");
        return phone;
    }
    public static void appointmentSlot(LocalDate date, LocalTime time) {
        if (date == null) throw new IllegalArgumentException("Choose an appointment date.");
        if (time == null) throw new IllegalArgumentException("Choose an appointment time.");
        if (!date.isAfter(LocalDate.now()) && !(date.equals(LocalDate.now()) && time.isAfter(LocalTime.now())))
            throw new IllegalArgumentException("Appointments must be booked for a future date and time.");
    }
}

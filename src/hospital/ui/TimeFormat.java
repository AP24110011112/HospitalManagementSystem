package hospital.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

/** 12-hour AM/PM formatting for display and clinic-hours entry. */
public final class TimeFormat {
    private static final DateTimeFormatter INPUT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("h:mm a").toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private TimeFormat() { }

    public static LocalTime parse(String value) { return LocalTime.parse(value.trim(), INPUT); }
    public static String format(LocalTime value) { return value == null ? "" : DISPLAY.format(value); }
}

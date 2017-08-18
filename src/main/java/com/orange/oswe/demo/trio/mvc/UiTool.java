package com.orange.oswe.demo.trio.mvc;

import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;

/**
 * Helper class used in some Thymeleaf views
 */
@Slf4j
public class UiTool {

	private final TimeZone timeZone;

	private final Locale locale;

	private final ResourceBundle messages;
	
	private final Instant now = Instant.now();
	
	public UiTool(TimeZone userTimeZone, Locale userLocale) {
		this.timeZone = userTimeZone;
		this.locale = userLocale;
		this.messages = ResourceBundle.getBundle("messages.messages", userLocale);
	}

	/**
	 * Format a date to a user friendly format:
	 * <ul>
	 * <li>if date less than one minute ago: "just now"
	 * <li>if date less than one hour ago: "XXX min."
	 * <li>if date was earlier today: "HH:mm"
	 * <li>if date was yesterday: "yesterday HH:mm"
	 * <li>if date was this year: "dd MMM"
	 * <li>else: "dd MMM YYYY"
	 * </ul>
	 * 
	 * @param date
	 *            date to format
	 * @return formatted date
	 */
	public String friendlyFormat(Instant date) {
		if (date == null) {
			return null;
		}
		long diff = now.toEpochMilli() - date.toEpochMilli();
		if (diff < 60000L) {
			// almost now
			return message("friendly_format.last_min");
		} else if (diff < 3600000L) {
			// less than one hour ago
			return messageAndArgs("friendly_format.last_hour", diff / 60000L);
		} else {
			ZoneId zone = timeZone.toZoneId();
			LocalDateTime localDate = LocalDateTime.ofInstant(date, zone);
			LocalDate dateDay = localDate.toLocalDate();
			LocalDateTime localNow = LocalDateTime.ofInstant(now, zone);
			LocalDate nowDay = localNow.toLocalDate();
			Period dateToNow = dateDay.until(nowDay);
			if (dateToNow.getYears() == 0 && dateToNow.getMonths() == 0 && dateToNow.getDays() == 0) {
				// today
				String hhmm = localDate.format(DateTimeFormatter.ofPattern(messages.getString("friendly_format.hm_format"), locale));
				return messageAndArgs("friendly_format.today", hhmm);
			} else if (dateToNow.getYears() == 0 && dateToNow.getMonths() == 0 && dateToNow.getDays() == 1) {
				// yesterday
				String hhmm = localDate.format(DateTimeFormatter.ofPattern(messages.getString("friendly_format.hm_format"), locale));
				return messageAndArgs("friendly_format.yesterday", hhmm);
			} else if (localDate.getYear() == localNow.getYear()) {
				// this year: DD MMM
				String ddMM = localDate.format(DateTimeFormatter.ofPattern(messages.getString("friendly_format.dM_format"), locale));
				return messageAndArgs("friendly_format.this_year", ddMM);
			} else {
				// another year: DD MMM YYYY
				String ddMM = localDate.format(DateTimeFormatter.ofPattern(messages.getString("friendly_format.dMY_format"), locale));
				return messageAndArgs("friendly_format.another_year", ddMM);
			}
		}
	}

	private String message(String key) {
		return message(key, '!' + key + '!');
	}

	private String message(String key, String defaultMsg) {
		try {
			return messages.getString(key);
		} catch (MissingResourceException e) {
			log.trace("Missing catalog key {}", key, e);
			return defaultMsg;
		}
	}

	private String messageAndArgs(String key, Object... params) {
		try {
			return MessageFormat.format(messages.getString(key), params);
		} catch (MissingResourceException e) {
			log.trace("Missing catalog key {}", key, e);
			return '!' + key + '!';
		}
	}
}

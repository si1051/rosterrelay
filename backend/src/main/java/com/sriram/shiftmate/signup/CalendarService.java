package com.sriram.shiftmate.signup;

import com.sriram.shiftmate.account.Account;
import com.sriram.shiftmate.account.AccountRepository;
import com.sriram.shiftmate.common.NotFoundException;
import com.sriram.shiftmate.shift.Shift;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** iCalendar (RFC 5545) feed so confirmed shifts show up in Google/Apple/Outlook calendars. */
@Service
@Transactional(readOnly = true)
public class CalendarService {

    private static final DateTimeFormatter ICS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final AccountRepository accounts;
    private final SignupRepository signups;
    private final Clock clock;

    public CalendarService(AccountRepository accounts, SignupRepository signups, Clock clock) {
        this.accounts = accounts;
        this.signups = signups;
        this.clock = clock;
    }

    public String feed(String token) {
        Account a = accounts.findByCalendarToken(token).orElseThrow(() -> new NotFoundException("Unknown calendar"));
        StringBuilder sb = new StringBuilder();
        line(sb, "BEGIN:VCALENDAR");
        line(sb, "VERSION:2.0");
        line(sb, "PRODID:-//ShiftMate//Volunteer Shifts//EN");
        line(sb, "CALSCALE:GREGORIAN");
        line(sb, "X-WR-CALNAME:ShiftMate - " + escape(a.getName()));
        for (Signup s : signups.findAllByVolunteerIdOrderByCreatedAtDesc(a.getId())) {
            if (s.getStatus() != SignupStatus.CONFIRMED && s.getStatus() != SignupStatus.ATTENDED) {
                continue;
            }
            Shift sh = s.getShift();
            line(sb, "BEGIN:VEVENT");
            line(sb, "UID:shiftmate-signup-" + s.getId() + "@shiftmate");
            line(sb, "DTSTAMP:" + ICS.format(clock.instant()));
            line(sb, "DTSTART:" + ICS.format(sh.getStartsAt()));
            line(sb, "DTEND:" + ICS.format(sh.getEndsAt()));
            line(sb, "SUMMARY:" + escape(sh.getTitle() + " (" + sh.getCoordinator().getOrganization() + ")"));
            line(sb, "LOCATION:" + escape(sh.getLocation()));
            if (sh.getDescription() != null) {
                line(sb, "DESCRIPTION:" + escape(sh.getDescription()));
            }
            line(sb, "BEGIN:VALARM");
            line(sb, "TRIGGER:-PT2H");
            line(sb, "ACTION:DISPLAY");
            line(sb, "DESCRIPTION:Volunteer shift in 2 hours");
            line(sb, "END:VALARM");
            line(sb, "END:VEVENT");
        }
        line(sb, "END:VCALENDAR");
        return sb.toString();
    }

    static String escape(String v) {
        return v.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    /** RFC 5545: CRLF line endings, lines folded at 75 octets. */
    private static void line(StringBuilder sb, String content) {
        String s = content;
        boolean first = true;
        while (s.length() > (first ? 75 : 74)) {
            int cut = first ? 75 : 74;
            sb.append(first ? "" : " ").append(s, 0, cut).append("\r\n");
            s = s.substring(cut);
            first = false;
        }
        sb.append(first ? "" : " ").append(s).append("\r\n");
    }
}

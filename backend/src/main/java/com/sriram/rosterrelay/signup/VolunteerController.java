package com.sriram.rosterrelay.signup;

import com.sriram.rosterrelay.auth.CurrentUser;
import com.sriram.rosterrelay.signup.SignupDtos.ShiftView;
import com.sriram.rosterrelay.signup.SignupDtos.SignupView;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VolunteerController {

    private final SignupService signups;
    private final HoursService hours;
    private final CalendarService calendar;
    private final CurrentUser currentUser;
    private final Clock clock;

    public VolunteerController(SignupService signups, HoursService hours, CalendarService calendar,
                               CurrentUser currentUser, Clock clock) {
        this.signups = signups;
        this.hours = hours;
        this.calendar = calendar;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @GetMapping("/shifts")
    public List<ShiftView> browse() {
        return signups.browse();
    }

    @PostMapping("/shifts/{id}/signup")
    public SignupView signUp(@PathVariable Long id) {
        return signups.signUp(id);
    }

    @GetMapping("/signups/mine")
    public List<SignupView> mine() {
        return signups.mine();
    }

    @PostMapping("/signups/{id}/cancel")
    public SignupView cancel(@PathVariable Long id) {
        return signups.cancel(id);
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        var a = currentUser.get();
        return Map.of("id", a.getId(), "name", a.getName(), "email", a.getEmail(), "role", a.getRole(),
                "organization", a.getOrganization() == null ? "" : a.getOrganization(),
                "calendarPath", "/api/calendar/" + a.getCalendarToken() + ".ics");
    }

    @PostMapping("/me/calendar/rotate")
    @Transactional
    public Map<String, String> rotateCalendar() {
        var a = currentUser.get();
        a.rotateCalendarToken();
        return Map.of("calendarPath", "/api/calendar/" + a.getCalendarToken() + ".ics");
    }

    @GetMapping("/me/hours")
    public HoursService.HoursSummary hours() {
        return hours.summary();
    }

    @GetMapping(value = "/me/hours.csv", produces = "text/csv")
    public ResponseEntity<String> hoursCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"volunteer-hours.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(hours.csv(clock.getZone()));
    }

    @GetMapping(value = "/calendar/{token}.ics", produces = "text/calendar")
    public ResponseEntity<String> calendarFeed(@PathVariable String token) {
        return ResponseEntity.ok().contentType(new MediaType("text", "calendar")).body(calendar.feed(token));
    }
}

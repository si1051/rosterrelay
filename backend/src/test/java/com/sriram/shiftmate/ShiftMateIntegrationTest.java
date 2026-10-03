package com.sriram.shiftmate;

import com.fasterxml.jackson.databind.JsonNode;
import com.sriram.shiftmate.notify.ReminderJob;
import com.sriram.shiftmate.shift.Shift;
import com.sriram.shiftmate.shift.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ShiftMateIntegrationTest {

    static final Duration H = Duration.ofHours(1);

    @Autowired MockMvc mvc;
    @Autowired ShiftRepository shifts;
    @Autowired ReminderJob reminders;
    Api api;
    String coord;

    @BeforeEach
    void setUp() throws Exception {
        api = new Api(mvc);
        coord = api.register("COORDINATOR", "Casey Coordinator");
    }

    private void startShiftInPast(long shiftId) {
        Shift s = shifts.findById(shiftId).orElseThrow();
        s.setStartsAt(Instant.now().minus(Duration.ofHours(3)));
        s.setEndsAt(Instant.now().minus(Duration.ofHours(1)));
        shifts.save(s);
    }

    @Test
    void fillsSeatsThenWaitlistsAndPromotesOnCancel() throws Exception {
        long shift = api.createShift(coord, "Food sort", Duration.ofDays(5), Duration.ofHours(3), 2);
        String a = api.register("VOLUNTEER", "Ava");
        String b = api.register("VOLUNTEER", "Ben");
        String c = api.register("VOLUNTEER", "Cara");
        String d = api.register("VOLUNTEER", "Dev");

        long sa = api.signUp(a, shift);
        api.signUp(b, shift);
        api.post(c, "/api/shifts/" + shift + "/signup")
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1));
        api.post(d, "/api/shifts/" + shift + "/signup").andExpect(jsonPath("$.waitlistPosition").value(2));

        api.post(a, "/api/signups/" + sa + "/cancel")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        // Cara was first in line: she's promoted and told
        api.get(c, "/api/signups/mine").andExpect(jsonPath("$[0].status").value("CONFIRMED"));
        api.get(c, "/api/me/notifications")
                .andExpect(jsonPath("$.unread").value(1))
                .andExpect(jsonPath("$.items[0].type").value("PROMOTED_FROM_WAITLIST"));
        api.get(d, "/api/signups/mine").andExpect(jsonPath("$[0].waitlistPosition").value(1));
    }

    @Test
    void lateCancellationIsRecorded() throws Exception {
        long soon = api.createShift(coord, "Soup kitchen", Duration.ofHours(5), Duration.ofHours(2), 3);
        String v = api.register("VOLUNTEER", "Lou");
        long s = api.signUp(v, soon);
        api.post(v, "/api/signups/" + s + "/cancel").andExpect(jsonPath("$.status").value("LATE_CANCELLED"));
    }

    @Test
    void blocksDoubleBookingAndDuplicates() throws Exception {
        long first = api.createShift(coord, "Morning shelter", Duration.ofDays(3), Duration.ofHours(4), 5);
        long overlapping = api.createShift(coord, "Pantry intake", Duration.ofDays(3).plusHours(2), Duration.ofHours(3), 5);
        long later = api.createShift(coord, "Evening shelter", Duration.ofDays(3).plusHours(5), Duration.ofHours(3), 5);
        String v = api.register("VOLUNTEER", "Max");
        api.signUp(v, first);

        api.post(v, "/api/shifts/" + first + "/signup").andExpect(status().isConflict());
        api.post(v, "/api/shifts/" + overlapping + "/signup")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("overlaps")));
        api.post(v, "/api/shifts/" + later + "/signup").andExpect(status().isOk());
    }

    @Test
    void attendanceCreditsHoursAndFeedsReliability() throws Exception {
        long shift = api.createShift(coord, "Park cleanup", Duration.ofDays(1), Duration.ofHours(2), 4);
        String v = api.register("VOLUNTEER", "Rae");
        String w = api.register("VOLUNTEER", "Sam");
        long sv = api.signUp(v, shift);
        long sw = api.signUp(w, shift);

        api.post(coord, "/api/coordinator/signups/" + sv + "/attendance", "{\"attended\":true}")
                .andExpect(status().isConflict()); // not started yet

        startShiftInPast(shift);
        api.post(coord, "/api/coordinator/signups/" + sv + "/attendance", "{\"attended\":true}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATTENDED"));
        api.post(coord, "/api/coordinator/signups/" + sw + "/attendance", "{\"attended\":false}")
                .andExpect(jsonPath("$.status").value("NO_SHOW"))
                .andExpect(jsonPath("$.reliability").value(lessThan(80)));

        api.get(v, "/api/me/hours")
                .andExpect(jsonPath("$.totalHours").value(2.0))
                .andExpect(jsonPath("$.shiftsAttended").value(1))
                .andExpect(jsonPath("$.byOrganization['Helping Hands']").value(2.0))
                .andExpect(jsonPath("$.reliability.score").value(greaterThan(80)));
        String csv = api.get(v, "/api/me/hours.csv").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(csv).startsWith("date,organization,shift,hours").contains("Park cleanup,2.0").contains("TOTAL,2.0");

        api.get(coord, "/api/coordinator/dashboard")
                .andExpect(jsonPath("$.volunteerHours").value(2.0))
                .andExpect(jsonPath("$.noShowRate").value(50.0));
    }

    @Test
    void dashboardFlagsUnderstaffedShiftsWithReliableSuggestions() throws Exception {
        long past = api.createShift(coord, "Past event", Duration.ofDays(1), Duration.ofHours(2), 2);
        String reliable = api.register("VOLUNTEER", "Reliable Rita");
        long sr = api.signUp(reliable, past);
        startShiftInPast(past);
        api.post(coord, "/api/coordinator/signups/" + sr + "/attendance", "{\"attended\":true}").andExpect(status().isOk());

        long upcoming = api.createShift(coord, "Saturday distribution", Duration.ofHours(30), Duration.ofHours(3), 10);
        JsonNode dash = Api.json(api.get(coord, "/api/coordinator/dashboard").andExpect(status().isOk()));
        JsonNode risk = null;
        for (JsonNode n : dash.get("atRisk")) {
            if (n.get("shiftId").asLong() == upcoming) {
                risk = n;
            }
        }
        assertThat(risk).isNotNull();
        assertThat(risk.get("gap").asInt()).isEqualTo(10);
        assertThat(risk.at("/suggestions/0/name").asText()).isEqualTo("Reliable Rita");

        long volunteerId = risk.at("/suggestions/0/volunteerId").asLong();
        api.post(coord, "/api/coordinator/shifts/" + upcoming + "/invite/" + volunteerId).andExpect(status().isNoContent());
        api.get(reliable, "/api/me/notifications").andExpect(jsonPath("$.items[0].type").value("INVITE"));
    }

    @Test
    void cancellingAShiftNotifiesEveryone() throws Exception {
        long shift = api.createShift(coord, "Gala setup", Duration.ofDays(4), Duration.ofHours(2), 1);
        String a = api.register("VOLUNTEER", "Ana");
        String b = api.register("VOLUNTEER", "Bo");
        api.signUp(a, shift);
        api.signUp(b, shift); // waitlisted
        api.post(coord, "/api/coordinator/shifts/" + shift + "/cancel").andExpect(status().isNoContent());
        api.get(a, "/api/me/notifications").andExpect(jsonPath("$.items[0].type").value("SHIFT_CANCELLED"));
        api.get(b, "/api/me/notifications").andExpect(jsonPath("$.items[0].type").value("SHIFT_CANCELLED"));
        api.post(a, "/api/shifts/" + shift + "/signup").andExpect(status().isConflict());
    }

    @Test
    void remindersGoOutOnceForShiftsInTheNextDay() throws Exception {
        long soon = api.createShift(coord, "Blood drive", Duration.ofHours(10), Duration.ofHours(2), 5);
        String v = api.register("VOLUNTEER", "Ivy");
        api.signUp(v, soon);
        assertThat(reminders.sendDue()).isGreaterThanOrEqualTo(1);
        int again = reminders.sendDue();
        api.get(v, "/api/me/notifications")
                .andExpect(jsonPath("$.items[?(@.type == 'REMINDER')]", hasSize(1)));
        assertThat(again).isZero();
    }

    @Test
    void calendarFeedIsPublicButUnguessable() throws Exception {
        long shift = api.createShift(coord, "Library tutoring, grades 3-5", Duration.ofDays(2), Duration.ofHours(2), 3);
        String v = api.register("VOLUNTEER", "Kai");
        api.signUp(v, shift);
        String path = Api.json(api.get(v, "/api/me").andExpect(status().isOk())).get("calendarPath").asText();

        String ics = mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andReturn().getResponse().getContentAsString();
        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").contains("BEGIN:VEVENT")
                .contains("SUMMARY:Library tutoring\\, grades 3-5 (Helping Hands)").endsWith("END:VCALENDAR\r\n");

        mvc.perform(get("/api/calendar/not-a-real-token.ics")).andExpect(status().isNotFound());
        String rotated = Api.json(api.post(v, "/api/me/calendar/rotate")).get("calendarPath").asText();
        assertThat(rotated).isNotEqualTo(path);
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @Test
    void enforcesRolesAndOwnership() throws Exception {
        long shift = api.createShift(coord, "Owned", Duration.ofDays(2), Duration.ofHours(2), 3);
        String v = api.register("VOLUNTEER", "Vic");
        String otherCoord = api.register("COORDINATOR", "Other Org");

        api.get(v, "/api/coordinator/dashboard").andExpect(status().isForbidden());
        api.get(otherCoord, "/api/coordinator/shifts/" + shift + "/roster").andExpect(status().isForbidden());
        api.post(coord, "/api/coordinator/shifts", """
                {"title":"Bad","location":"X","startsAt":"2030-01-01T10:00:00Z","endsAt":"2030-01-01T09:00:00Z","capacity":0}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.durationValid").exists())
                .andExpect(jsonPath("$.errors.capacity").exists());
        mvc.perform(get("/api/shifts")).andExpect(status().isUnauthorized());
    }
}

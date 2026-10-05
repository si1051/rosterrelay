package com.sriram.rosterrelay.shift;

import com.sriram.rosterrelay.insights.CoordinatorInsights;
import com.sriram.rosterrelay.shift.ShiftDtos.ShiftRequest;
import com.sriram.rosterrelay.signup.SignupDtos.RosterEntry;
import com.sriram.rosterrelay.signup.SignupDtos.ShiftView;
import com.sriram.rosterrelay.signup.SignupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coordinator")
@PreAuthorize("hasRole('COORDINATOR')")
public class CoordinatorController {

    private final ShiftService shifts;
    private final SignupService signups;
    private final CoordinatorInsights insights;

    public CoordinatorController(ShiftService shifts, SignupService signups, CoordinatorInsights insights) {
        this.shifts = shifts;
        this.signups = signups;
        this.insights = insights;
    }

    public record AttendanceRequest(boolean attended) {
    }

    @GetMapping("/shifts")
    public List<ShiftView> myShifts() {
        return shifts.mine();
    }

    @PostMapping("/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftView create(@Valid @RequestBody ShiftRequest request) {
        Shift s = shifts.create(request);
        return ShiftView.of(s, 0, 0, null, null);
    }

    @PostMapping("/shifts/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        shifts.cancel(id);
    }

    @GetMapping("/shifts/{id}/roster")
    public List<RosterEntry> roster(@PathVariable Long id) {
        return signups.roster(id);
    }

    @PostMapping("/signups/{id}/attendance")
    public RosterEntry attendance(@PathVariable Long id, @RequestBody AttendanceRequest request) {
        return signups.markAttendance(id, request.attended());
    }

    @GetMapping("/dashboard")
    public CoordinatorInsights.Dashboard dashboard() {
        return insights.dashboard();
    }

    @PostMapping("/shifts/{shiftId}/invite/{volunteerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invite(@PathVariable Long shiftId, @PathVariable Long volunteerId) {
        insights.invite(shiftId, volunteerId);
    }
}

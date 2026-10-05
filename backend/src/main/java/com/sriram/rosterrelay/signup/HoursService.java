package com.sriram.rosterrelay.signup;

import com.sriram.rosterrelay.auth.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HoursService {

    private final SignupRepository signups;
    private final CurrentUser currentUser;

    public HoursService(SignupRepository signups, CurrentUser currentUser) {
        this.signups = signups;
        this.currentUser = currentUser;
    }

    public record HoursEntry(Long signupId, String shift, String organization, Instant date, double hours) {
    }

    public record HoursSummary(double totalHours, int shiftsAttended, Map<String, Double> byOrganization,
                               Reliability reliability, List<HoursEntry> entries) {
    }

    public HoursSummary summary() {
        List<Signup> all = signups.findAllByVolunteerIdOrderByCreatedAtDesc(currentUser.id());
        List<HoursEntry> entries = all.stream()
                .filter(s -> s.getStatus() == SignupStatus.ATTENDED)
                .sorted(Comparator.comparing((Signup s) -> s.getShift().getStartsAt()).reversed())
                .map(s -> new HoursEntry(s.getId(), s.getShift().getTitle(), s.getShift().getCoordinator().getOrganization(),
                        s.getShift().getStartsAt(), s.getHoursCredited()))
                .toList();
        Map<String, Double> byOrg = entries.stream().collect(Collectors.groupingBy(HoursEntry::organization,
                TreeMap::new, Collectors.summingDouble(HoursEntry::hours)));
        byOrg.replaceAll((k, v) -> Math.round(v * 100) / 100.0);
        double total = Math.round(entries.stream().mapToDouble(HoursEntry::hours).sum() * 100) / 100.0;
        return new HoursSummary(total, entries.size(), byOrg, Reliability.of(all), entries);
    }

    /** Service-hours log suitable for school, court or employer volunteer-hour requirements. */
    public String csv(ZoneId zone) {
        DateTimeFormatter d = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone);
        StringBuilder sb = new StringBuilder("date,organization,shift,hours\n");
        HoursSummary s = summary();
        for (HoursEntry e : s.entries()) {
            sb.append(d.format(e.date())).append(',').append(cell(e.organization())).append(',')
                    .append(cell(e.shift())).append(',').append(e.hours()).append('\n');
        }
        sb.append(",,TOTAL,").append(s.totalHours()).append('\n');
        return sb.toString();
    }

    static String cell(String v) {
        if (v == null) {
            return "";
        }
        String x = !v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 ? "'" + v : v;
        return x.contains(",") || x.contains("\"") || x.contains("\n") ? "\"" + x.replace("\"", "\"\"") + "\"" : x;
    }
}

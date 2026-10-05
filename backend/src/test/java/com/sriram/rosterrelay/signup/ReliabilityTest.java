package com.sriram.rosterrelay.signup;

import com.sriram.rosterrelay.account.Account;
import com.sriram.rosterrelay.account.Role;
import com.sriram.rosterrelay.shift.Shift;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReliabilityTest {

    static List<Signup> history(int attended, int noShows, int late) {
        Account v = new Account("v@x.y", "h", "V", Role.VOLUNTEER);
        Shift s = new Shift(new Account("c@x.y", "h", "C", Role.COORDINATOR));
        List<Signup> out = new ArrayList<>();
        for (int i = 0; i < attended; i++) out.add(new Signup(s, v, SignupStatus.ATTENDED, Instant.EPOCH));
        for (int i = 0; i < noShows; i++) out.add(new Signup(s, v, SignupStatus.NO_SHOW, Instant.EPOCH));
        for (int i = 0; i < late; i++) out.add(new Signup(s, v, SignupStatus.LATE_CANCELLED, Instant.EPOCH));
        out.add(new Signup(s, v, SignupStatus.CANCELLED, Instant.EPOCH)); // early cancels don't count
        return out;
    }

    @Test
    void newcomersStartHighButNotPerfect() {
        assertThat(Reliability.of(history(0, 0, 0)).score()).isEqualTo(80);
    }

    @Test
    void oneMissIsForgivenMoreThanAPattern() {
        int oneMiss = Reliability.of(history(5, 1, 0)).score();
        int pattern = Reliability.of(history(2, 4, 0)).score();
        assertThat(oneMiss).isGreaterThan(75);
        assertThat(pattern).isLessThan(50);
    }

    @Test
    void lateCancelsCountAsHalfANoShow() {
        assertThat(Reliability.of(history(4, 0, 2)).score())
                .isGreaterThan(Reliability.of(history(4, 2, 0)).score());
    }

    @Test
    void steadyVolunteersApproachHundred() {
        assertThat(Reliability.of(history(30, 0, 0)).score()).isGreaterThanOrEqualTo(98);
    }
}

package pt.ua.tqs.tuttibox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically applies the drop-off and pickup deadlines. Tests call ReservationService.applyDeadlines() directly. */
@Component
public class DeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineScheduler.class);

    private final ReservationService reservations;

    public DeadlineScheduler(ReservationService reservations) {
        this.reservations = reservations;
    }

    @Scheduled(fixedDelayString = "${tuttibox.scheduler.deadline-check-ms:60000}",
            initialDelayString = "${tuttibox.scheduler.deadline-check-ms:60000}")
    public void run() {
        int changed = reservations.applyDeadlines();
        if (changed > 0) {
            log.info("Deadline check: {} parcel(s) changed state", changed);
        }
    }
}

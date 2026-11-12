package rs.raf.lms.teacher.dto.response;

public enum MonitoringStatus {
    NOT_STARTED,   // nije preuzeo zadatak
    IN_PROGRESS,   // preuzeo je, a nije predao sve
    SUBMITTED      // predao je finalno rešenje za sve svoje zadatke
}
package org.example.dahuasdk.entity;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "utc_event_time", nullable = false)
    private int UTCEventTime;

    @Column(name = "event_code", nullable = false)
    private String eventCode;

    @Column(name = "person_code")
    private String personCode;

    @Column(name = "card_no")
    private String cardNo;
}

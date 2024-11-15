package org.example.dahuasdk.dao;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.dahuasdk.entity.Event;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class EventDAO {
    private final EntityManager entityManager;

    public void bulkInsertEvents(List<Event> events) {
        for (Event event: events) {
            entityManager.persist(event);
        }
    }
}

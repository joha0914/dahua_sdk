package org.example.dahuasdk.services;

import lombok.RequiredArgsConstructor;
import org.example.dahuasdk.client.vhr.VHRClient;
import org.example.dahuasdk.dao.AppDAO;
import org.example.dahuasdk.dto.EventDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class EventProcessorService {
    private final VHRClient vhrClient;
    private final AppDAO appDAO;

    @Transactional(readOnly = true)
    public void processEvent(EventDTO event) {
        var middleware = appDAO.findMiddlewareByDeviceId(event.getDeviceId());
        vhrClient.sendEvents(middleware, List.of(event));
    }

    @Transactional(readOnly = true)
    public void processEvents(List<EventDTO> events, String deviceId) {
        var middleware = appDAO.findMiddlewareByDeviceId(deviceId);
        vhrClient.sendEvents(middleware, events);
    }
}

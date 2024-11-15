package org.example.dahuasdk.dao;


import org.example.dahuasdk.entity.Device;
import org.example.dahuasdk.entity.Middleware;

import java.util.List;

public interface AppDAO {
    void createMiddleware(Middleware middleware);
    List<Middleware> findAllMiddleware();
    Middleware findMiddlewareById(long id);
    Middleware findMiddlewareByToken(String token);
    void updateMiddleware(Middleware middleware);
    void deleteMiddlewareById(long id);
    void saveDevice(Device device);
    List<Device> findAllDevicesByMiddlewareId(long middlewareId);
    Device findDeviceById(long id);
    void updateDevice(Device device);
    void deleteDeviceById(long id);
    void deleteDeviceByMiddlewareIdAndVhrId(long middlewareId, long vhrId);
    Device findDeviceByMiddlewareIdAndVhrId(long middlewareId, long vhrId);
    boolean existsDeviceByMiddlewareIdAndVhrId(long middlewareId, long vhrId);
    Device findDeviceByDeviceId(String deviceId);
    Middleware findMiddlewareByDeviceId(String deviceId);
}
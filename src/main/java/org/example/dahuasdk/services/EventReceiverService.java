package org.example.dahuasdk.services;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;
import lombok.RequiredArgsConstructor;
import org.example.dahuasdk.dto.EventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EventReceiverService {
    private static final Logger log = LoggerFactory.getLogger(EventReceiverService.class);
    private final EventProcessorService eventProcessor;

    // TODO: think if you need to release handle on device disconnect
    private NetSDKLib.LLong eventListeningStart(NetSDKLib netSDKInstance, int channelId, NetSDKLib.LLong loginHandle, String deviceId) {
        int bNeedPicture = 1; // include photo info in events

        NetSDKLib.LLong eventListenerHandle = netSDKInstance.CLIENT_RealLoadPictureEx(loginHandle, channelId, NetSDKLib.EVENT_IVS_ALL,
                bNeedPicture, new AnalyzerDataCB(deviceId, eventProcessor), null, null);
        if(eventListenerHandle.longValue() != 0) {
            log.debug("CLIENT_RealLoadPictureEx Success. DeviceId: {}, ChannelId : {}", deviceId, channelId);
        } else {
            log.error("CLIENT_RealLoadPictureEx Failed! Error Code: {}", ToolKits.getErrorCodePrint());
            return null;
        }

        return eventListenerHandle;
    }

    public NetSDKLib.LLong eventListeningStart(NetSDKLib netSDKInstance, NetSDKLib.LLong loginHandle, String deviceId) {
        int channelId = -1; // All channels
        NetSDKLib.LLong eventListenerHandle = null;

        try {
            eventListenerHandle = eventListeningStart(netSDKInstance, channelId, loginHandle, deviceId);

            if (eventListenerHandle == null) {
                channelId = 0; // First channel

                eventListenerHandle = eventListeningStart(netSDKInstance, channelId, loginHandle, deviceId);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        return eventListenerHandle;
    }

    private record AnalyzerDataCB(String deviceId, EventProcessorService eventProcessor) implements NetSDKLib.fAnalyzerDataCallBack {
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType,
                          Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle.longValue() == 0 || pAlarmInfo == null) {
                return -1;
            }

            // ignore all events, except access control events
            if (dwAlarmType != NetSDKLib.EVENT_IVS_ACCESS_CTL) {
                return 0;
            }

            int resultCode = 0;

            try {
                NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO msg = new NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);

                // TODO: add image processing logic
                // File path = new File("./GateSnapPicture/");
                // if (!path.exists()) {
                //     path.mkdir();
                // }
                //
                // String snapPicPath = path + "\\" + System.currentTimeMillis() + "GateSnapPicture.jpg";  // 保存图片地址
                // byte[] buffer = pBuffer.getByteArray(0, dwBufSize);
                // ByteArrayInputStream byteArrInputGlobal = new ByteArrayInputStream(buffer);
                //
                // try {
                //     BufferedImage gateBufferedImage = ImageIO.read(byteArrInputGlobal);
                //     if(gateBufferedImage != null) {
                //         ImageIO.write(gateBufferedImage, "jpg", new File(snapPicPath));
                //     }
                // } catch (IOException e2) {
                //     e2.printStackTrace();
                // }

                var event = new EventDTO(msg, this.deviceId);

                eventProcessor.processEvent(event);
            } catch (Exception ex) {
                log.error(ex.getMessage());
                resultCode = -1;
            }


            return resultCode;
        }
    }
}

package org.example.dahuasdk.services;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import lombok.RequiredArgsConstructor;
import org.example.dahuasdk.dto.EventDTO;
import org.example.dahuasdk.exceptions.EventLoadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class EventLoaderService {
    private static final Logger log = LoggerFactory.getLogger(EventLoaderService.class);
    private final EventProcessorService eventProcessor;

    public List<EventDTO> findAccessRecords(
            NetSDKLib netSDKInstance,
            NetSDKLib.LLong loginHandle,
            String deviceId,
            int eventStartLoadUTCTime,
            int eventEndLoadUTCTime
    ) {
        List<EventDTO> eventDTOList = new ArrayList<>();

        NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = null;

        try {
            NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX recordCondition = new NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
            recordCondition.bRealUTCTimeEnable = 1;
            recordCondition.nStartRealUTCTime = eventStartLoadUTCTime;
            recordCondition.nEndRealUTCTime = eventEndLoadUTCTime;

            NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
            stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;
            stuFindInParam.pQueryCondition = recordCondition.getPointer();

            stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();
            recordCondition.write();

            if (!netSDKInstance.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) {
                throw new EventLoadingException("Cannot Find Record. Error Code: " + String.format("0x%x", netSDKInstance.CLIENT_GetLastError()));
            }

            log.debug("FindRecord Succeeded\nFindHandle: {}", stuFindOutParam.lFindeHandle);

            recordCondition.read();
            int count = 0;
            final int nRecordCount = 1000;

            NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[] pstRecord = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[nRecordCount];
            for (int i = 0; i < nRecordCount; i++) {
                pstRecord[i] = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC();
            }

            NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
            stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
            stuFindNextInParam.nFileCount = nRecordCount;

            NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
            stuFindNextOutParam.nMaxRecordNum = nRecordCount;
            stuFindNextOutParam.pRecordList = new Memory((long) pstRecord[0].dwSize * nRecordCount);
            stuFindNextOutParam.pRecordList.clear((long) pstRecord[0].dwSize * nRecordCount);
            ToolKits.SetStructArrToPointerData(pstRecord, stuFindNextOutParam.pRecordList);

            while (true) {
                if (!netSDKInstance.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000)) {
                    throw new EventLoadingException("FindNextRecord Failed. Error Code: " + netSDKInstance.CLIENT_GetLastError());
                }

                ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, pstRecord);

                for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; i++) {
                    eventDTOList.add(new EventDTO(pstRecord[i], deviceId));
                }

                if (stuFindNextOutParam.nRetRecordNum < nRecordCount) {
                    break;
                } else {
                    count++;
                }
            }
        }
        catch (EventLoadingException ex) {
            log.error(ex.getMessage());
        }
        finally {
            if (stuFindOutParam != null) {
                netSDKInstance.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
            }
        }

        return eventDTOList;
    }

    public void loadAccessRecords(
        NetSDKLib netSDKInstance,
        NetSDKLib.LLong loginHandle,
        String deviceId,
        int eventStartLoadUTCTime,
        int eventEndLoadUTCTime
    ) {
        try {
            var events = findAccessRecords(netSDKInstance, loginHandle, deviceId, eventStartLoadUTCTime, eventEndLoadUTCTime);
            eventProcessor.processEvents(events, deviceId);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }
}

package jaja;

import java.util.Set;

import com.sun.research.ws.wadl.Application;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;

public interface OVSPipelineService {

    void initializeService(ApplicationId appId);

    void initializePipeline(Device device);

    void initializePipeline(DeviceService deviceService);

    void closePipeline();

    void setRule();

    void setUpTableMissEntry(Device device, int table);

    void connectTables(Device device, int fromTable, int toTable);

    void setStatsTpPorts(Device device, Set<Integer> port);

    void setRequestFlow(Device device);

    void setStatsTpPort(Device device, int port);
}

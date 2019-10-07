package jaja;

import java.util.Set;


import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;

public interface OVSPipelineService {

    void initializeService(ApplicationId appId);

    void initializePipeline();

    void setUpTableMissEntry(Device device, int table, int op);

    void connectTables(Device device, int fromTable, int toTable);

    // void setStatsTpPorts(Device device, Set<Integer> port);

    void setRequestFlow(Device device);

    // void setStatsTpPort(Device device, int port);

    Set<Integer> getTargetPorts();

    // void setTargetPorts(int port);

    void addStatsTpPort(int port);

    void addStatsTpPorts(Set<Integer> port);
}

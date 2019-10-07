package jaja;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.core.ApplicationId;

public interface StatsService {

    void initializeService(ApplicationId appId);

    Map<DeviceId, Map<Port, portStatsReaderTask>> getStatsTask();

    Map<DeviceId, Map<Port, ArrayList<Long>>> getRecord();

    Map<DeviceId, Map<Port, ArrayList<Float>>> getRate();

    List<Link> getCongestLinks();

    // void setDeviceService(DeviceService deviceService);

    // void setLinkService(LinkService linkService);

    // void setTopologyService(TopologyService topologyService);

    // DeviceService getDeviceService();

    // LinkService getLinkService();

    // TopologyService getTopologyService();
    
    void initializeStatsTask();

    void startTask();

    void stopService();
}
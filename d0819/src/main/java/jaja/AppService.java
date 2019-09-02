/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jaja;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

/**
 * Skeletal ONOS application API.
 */
public interface AppService {

    void initializeAppService(Logger log, DeviceService deviceService, LinkService linkService, TopologyService TopologyService);

    Map<DeviceId, Map<MacAddress, PortNumber>> getMacTable();
    
    void clearMacTable();

    void initializeMacTable();

    Map<DeviceId, Map<Port, portStatsReaderTask>> getStatsTask();

    Map<DeviceId, Map<Port, ArrayList<Long>>> getRecord();

    Map<DeviceId, Map<Port, ArrayList<Float>>> getRate();

    List<Link> getCongestLinks();

    void setDeviceService(DeviceService deviceService);

    void setLinkService(LinkService linkService);

    void setTopologyService(TopologyService topologyService);

    DeviceService getDeviceService();

    LinkService getLinkService();

    TopologyService getTopologyService();
    
    void initializeStatsTask();

    void startTask();

    void stopTask();


    
}

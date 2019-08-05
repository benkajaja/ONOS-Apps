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

// import com.google.common.collect.ImmutableSet;
// import com.google.common.collect.Maps;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;

import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

import java.lang.System;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Port statistics polling
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Some configurable property. */
    // private String someProperty;
    private ApplicationId appId;
    private Map<ConnectPoint,portStatsReaderTask> map = new HashMap<ConnectPoint,portStatsReaderTask>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Activate
    protected void activate() {
        // cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.jaja.portstats");
        Iterable<Device> devices = deviceService.getDevices();

        log.info("log name: {}", log.getName());
        log.info("appid: {} {}", appId.id(), appId.name());
        log.info("Hello");
        System.out.println("test");

        for(Device d : devices)
        {
            log.info("[DeviceID] {}", d.id().toString());

            List<Port> ports = deviceService.getPorts(d.id());
            for(Port port : ports) {
                log.info("  [PortID] {}", port.number());
                PortStatistics portstat = deviceService.getStatisticsForPort(d.id(), port.number());
                PortStatistics portdeltastat = deviceService.getDeltaStatisticsForPort(d.id(), port.number());
                if(portstat != null){
                    log.info("    Recieved {} bytes", portstat.bytesReceived());
                    log.info("    Recieved {} packets", portstat.packetsReceived());
                } else
                    log.warn("    Unable to read portStats");

                if(portdeltastat != null) {
                    log.info("    Recieved {} bytes --Delta", portdeltastat.bytesReceived());
                    log.info("    Recieved {} packets --Delta", portdeltastat.packetsReceived());
                } else
                    log.warn("    Unable to read portDeltaStats");
            }
            
            // this list doesn't contain LOCAL port of device
            List<PortStatistics> portStatisticsList = deviceService.getPortDeltaStatistics(d.id());
            for (PortStatistics portStats : portStatisticsList) {
                try {
                    PortNumber port = portStats.portNumber();
                    ConnectPoint cp = new ConnectPoint(d.id(), port);
                    portStatsReaderTask task = new portStatsReaderTask();
                    task.setDelay(3);
                    task.setExit(false);
                    task.setLog(log);
                    task.setPort(port);
                    task.setDeviceService(deviceService);
                    task.setDevice(d);
                    map.put(cp, task);
                    log.info("[Start] polling {} {}", task.getDevice().id(), task.getPort());
                    task.schedule();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("exception!");
                }
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        for(portStatsReaderTask task : map.values()) {
            log.info("[Stop] polling {} {}", task.getDevice().id(), task.getPort());
            task.setExit(true);
            task.getTimer().cancel();
        }
        log.info("Get Out");
    }

}

package jaja;

import java.util.Map;
import com.google.common.collect.Maps;

import org.onlab.packet.MacAddress;
import org.onosproject.core.Application;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.Component;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = AppService.class)
public class AppServiceImpl implements AppService {

    private Map<DeviceId, Map<MacAddress, PortNumber>> macTables;
    private final Logger log = LoggerFactory.getLogger("jaja.AppComponent");

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topoService;

    @Override
    public void initializeAppService(Application appId) {
        log.info("Initialize AppService");
    }

    @Override
    public Map<DeviceId, Map<MacAddress, PortNumber>> getMacTable(){
        return this.macTables;
    }

    @Override
    public void clearMacTable(){
        this.macTables = Maps.newConcurrentMap();
    }

    @Override
    public void initializeMacTable(){
        this.macTables = Maps.newConcurrentMap();
    }

}

/**
 * ref
 * DeviceService.class
 * DeviceServiceAdapter.class
 * DevicePortStatsCommand.class
 * DevicesListCommand.class
 * MetricsService.class
 * MetricsManager.class
 * 
 * OpenstackFlowRuleManager.java
 * OpenstackNodeService.java
 * OpenstackNodeListCommand.java
 */

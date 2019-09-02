
package jaja;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.Maps;
// import static com.google.common.collect.Lists.newArrayList;

import org.onlab.packet.MacAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.utils.Comparators;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(immediate = true, service = AppService.class)
public class AppServiceImpl implements AppService {

    private Map<DeviceId, Map<MacAddress, PortNumber>> macTables;
    // private Map<ConnectPoint, portStatsReaderTask> statsTasks;
    // private Map<ConnectPoint, ArrayList<Long>> records;

    private Map<DeviceId, Map<Port, portStatsReaderTask>> statsTasksA;
    private Map<DeviceId, Map<Port, ArrayList<Long>>> recordsA;
    private Map<DeviceId, Map<Port, ArrayList<Float>>> rates;
    // private Map<DeviceId, Map<Port, Map<MacAddress, Long>>> recordsB;
    private List<Link> congestLinks;

    
    private DeviceService deviceService;
    private LinkService linkService;
    private TopologyService topologyService;
    private Logger log;

    @Override
    public void initializeAppService(Logger log, 
                                     DeviceService deviceService, 
                                     LinkService linkService,
                                     TopologyService topologyService){
        this.log = log;
        
        this.deviceService = deviceService;
        this.linkService = linkService;
        this.topologyService = topologyService;
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

    @Override
    public Map<DeviceId, Map<Port, portStatsReaderTask>> getStatsTask(){
        return this.statsTasksA;
    }

    @Override
    public Map<DeviceId, Map<Port, ArrayList<Long>>> getRecord(){
        return this.recordsA;
    }

    @Override
    public Map<DeviceId, Map<Port, ArrayList<Float>>> getRate(){
        return this.rates;
    }

    @Override
    public List<Link> getCongestLinks(){
        return this.congestLinks;
    }

    @Override
    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public void setLinkService(LinkService linkService) {
        this.linkService = linkService;
    }

    @Override
    public void setTopologyService(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @Override
    public DeviceService getDeviceService() {
        return this.deviceService;
    }

    @Override
    public LinkService getLinkService(){
        return this.linkService;
    }
    
    @Override
    public TopologyService getTopologyService(){
        return this.topologyService;
    }



    @Override
    public void initializeStatsTask(){
        this.statsTasksA = Maps.newConcurrentMap();
        this.recordsA = Maps.newConcurrentMap();
        this.rates = Maps.newConcurrentMap();
        this.congestLinks = new ArrayList<Link>();
        log.info("[DEBUG] ini");
    }

    @Override
    public void startTask(){

        Iterable<Device> devices = deviceService.getDevices();
        // Iterable<Device> devices = deviceService.getAvailableDevices();

        // List<Device> devices = newArrayList(deviceService.getDevices());
        // Collections.sort(devices, Comparators.ELEMENT_COMPARATOR);
        

        for(Device d : devices)
        {
            Map<Port, portStatsReaderTask> newstatsMap = Maps.newConcurrentMap();
            Map<Port, ArrayList<Long>> newportsMap = Maps.newConcurrentMap();
            Map<Port, ArrayList<Float>> newratesMap = Maps.newConcurrentMap();
            statsTasksA.put(d.id(), newstatsMap);
            recordsA.put(d.id(), newportsMap);
            rates.put(d.id(), newratesMap);

            List<Port> ports = deviceService.getPorts(d.id());

            // List<Port> ports = newArrayList(deviceService.getPorts(d.id()));
            // Collections.sort(ports, Comparators.PORT_COMPARATOR);

            log.info("[DeviceID] {}", d.id().toString());

            for (Port port : ports) {
                try {
                    PortNumber portNum = port.number();
                    if (portNum.equals(PortNumber.LOCAL)) continue;
                    
                    ArrayList<Long> record = new ArrayList<Long>(4);
                    ArrayList<Float> rate = new ArrayList<Float>(2);
                    record.add(Long.valueOf(0));record.add(Long.valueOf(1));record.add(Long.valueOf(2));record.add(Long.valueOf(3));
                    rate.add(Float.valueOf(0));rate.add(Float.valueOf(1));
                    portStatsReaderTask task = new portStatsReaderTask();
                    task.setDelay(5);
                    task.setExit(false);
                    task.setLog(log);
                    task.setPort(portNum);
                    task.setDeviceService(deviceService);
                    task.setDevice(d);
                    task.setRecords(record);
                    task.setRates(rate);
                    task.setShowStatsLog(false);
                    task.setStatsType(portStatsReaderTask.DELTA);
                    task.setCongestLinks(congestLinks);
                    task.setLinkService(linkService);

                    newstatsMap.put(port, task);
                    newportsMap.put(port, record);
                    newratesMap.put(port, rate);

                    log.info("[Start] polling {} {}", task.getDevice().id(), task.getPort());
                    task.schedule();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("exception!");
                }
            }
        }
    }

    @Override
    public void stopTask(){

        for(Map<Port, portStatsReaderTask> p : statsTasksA.values()){
            for(portStatsReaderTask task : p.values()) {
                log.info("[Stop] polling {} {}", task.getDevice().id(), task.getPort());
                task.setExit(true);
                task.getTimer().cancel();
            }
        }
        
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

 /**
  * Copy 
  */
  
// package jaja;

// import java.util.ArrayList;
// import java.util.List;
// // import java.util.HashMap;
// import java.util.Map;
// import com.google.common.collect.Maps;

// import org.onlab.packet.MacAddress;
// import org.onosproject.net.ConnectPoint;
// import org.onosproject.net.Device;
// import org.onosproject.net.DeviceId;
// import org.onosproject.net.Port;
// import org.onosproject.net.PortNumber;
// import org.onosproject.net.device.DeviceService;
// import org.osgi.service.component.annotations.Component;
// import org.slf4j.Logger;

// @Component(immediate = true, service = AppService.class)
// public class AppServiceImpl implements AppService {

//     private Map<DeviceId, Map<MacAddress, PortNumber>> macTables;
//     private Map<ConnectPoint, portStatsReaderTask> statsTasks;
//     private Map<ConnectPoint, ArrayList<Long>> records;


    
//     private DeviceService deviceService;
//     private Logger log;

//     @Override
//     public void initializeAppService(Logger log){
//         this.log = log;
//     }

//     @Override
//     public Map<DeviceId, Map<MacAddress, PortNumber>> getMacTable(){
//         return this.macTables;
//     }

//     @Override
//     public void clearMacTable(){
//         this.macTables = Maps.newConcurrentMap();
//     }

//     @Override
//     public void initializeMacTable(){
//         this.macTables = Maps.newConcurrentMap();
//     }

//     @Override
//     public Map<ConnectPoint,portStatsReaderTask> getStatsTask(){
//         return this.statsTasks;
//     }

//     @Override
//     public Map<ConnectPoint, ArrayList<Long>> getRecord(){
//         return this.records;
//     }

//     @Override
//     public void initializeStatsTask(DeviceService deviceService){
//         this.statsTasks = Maps.newConcurrentMap();
//         this.records = Maps.newConcurrentMap();
//         this.deviceService = deviceService;
//         log.info("[DEBUG] ini");
//     }

//     @Override
//     public void startTask(){

//         Iterable<Device> devices = deviceService.getDevices();

//         for(Device d : devices)
//         {
//             log.info("[DeviceID] {}", d.id().toString());

//             List<Port> ports = deviceService.getPorts(d.id());
            
//             for (Port port : ports) {
//                 try {
//                     PortNumber portNum = port.number();
//                     ConnectPoint cp = new ConnectPoint(d.id(), portNum);
//                     ArrayList<Long> record = new ArrayList<Long>(4);
//                     record.add(Long.valueOf(0));record.add(Long.valueOf(1));record.add(Long.valueOf(2));record.add(Long.valueOf(3));
//                     portStatsReaderTask task = new portStatsReaderTask();
//                     task.setDelay(5);
//                     task.setExit(false);
//                     task.setLog(log);
//                     task.setPort(portNum);
//                     task.setDeviceService(deviceService);
//                     task.setDevice(d);
//                     task.setRecords(record);

//                     statsTasks.put(cp, task);
//                     records.put(cp, record); 
                    
//                     log.info("[Start] polling {} {}", task.getDevice().id(), task.getPort());
//                     task.schedule();
//                 } catch (Exception e) {
//                     e.printStackTrace();
//                     log.error("exception!");
//                 }
//             }
//         }
//     }

//     @Override
//     public void stopTask(){

//         for(portStatsReaderTask task : statsTasks.values()) {
//             log.info("[Stop] polling {} {}", task.getDevice().id(), task.getPort());
//             task.setExit(true);
//             task.getTimer().cancel();
//         }

//     }
    


// }

// /**
//  * ref
//  * DeviceService.class
//  * DeviceServiceAdapter.class
//  * DevicePortStatsCommand.class
//  * DevicesListCommand.class
//  * MetricsService.class
//  * MetricsManager.class
//  * 
//  * OpenstackFlowRuleManager.java
//  * OpenstackNodeService.java
//  * OpenstackNodeListCommand.java
//  */
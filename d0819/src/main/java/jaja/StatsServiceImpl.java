package jaja;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = StatsService.class)
public class StatsServiceImpl implements StatsService {

    private final Logger log = LoggerFactory.getLogger("jaja.AppComponent");
    private Map<DeviceId, Map<Port, portStatsReaderTask>> statsTasksA;
    private Map<DeviceId, Map<Port, ArrayList<Long>>> recordsA;
    private Map<DeviceId, Map<Port, ArrayList<Float>>> rates;
    private List<Link> congestLinks;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topoService;

    @Override
    public void initializeService(ApplicationId appId) {
        initializeStatsTask();
        startTask();
        log.info("Initialize StatsService");
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
    public void initializeStatsTask() {
        this.statsTasksA = Maps.newConcurrentMap();
        this.recordsA = Maps.newConcurrentMap();
        this.rates = Maps.newConcurrentMap();
        this.congestLinks = new ArrayList<Link>();
    }

    @Override
    public void startTask() {
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
    public void stopService() {
        for(Map<Port, portStatsReaderTask> p : statsTasksA.values()){
            for(portStatsReaderTask task : p.values()) {
                log.info("[Stop] polling {} {}", task.getDevice().id(), task.getPort());
                task.setExit(true);
                task.getTimer().cancel();
            }
        }
    }



}
package jaja;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = OVSPipelineService.class)
public class OVSPipelineServiceImpl implements OVSPipelineService {


    protected DeviceId deviceId;
    protected ApplicationId appId;
    private static final int ENTRY_TABLE = 0;
    private static final int L3FWD_TABLE = 2;
    private static final int L4STATS_TABLE = 1;
    private static final int TABLE_MISS_PRIORITY = 0;
    private final Logger log = LoggerFactory.getLogger("jaja.AppComponent");
    private static final int DEFAULT_PRIORITY = 10;
    // private static final int DEFAULT_DURATION = 20;
    private static final int DROP = 0;
    private static final int CONTROLLER = 1;
    private Set<Integer> targetPorts = Stream.of(5001,8787).collect(Collectors.toSet());
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Override
    public void initializeService(ApplicationId appId){
        this.appId = appId;
        initializePipeline();
        log.info("Initialize OVSPipelineService");
    }

    @Override
    public void initializePipeline() {

        for (Device dev: deviceService.getAvailableDevices()){
            log.info("test log");
            connectTables(dev, L4STATS_TABLE, L3FWD_TABLE);
            setRequestFlow(dev);
            // setStatsTables(dev, Set.of(56,8787));
            // setStatsTpPorts(dev, Stream.of(5001,8787).collect(Collectors.toSet()));
            setStatsTpPorts(dev, targetPorts);
            setStatsTpPort(dev, 9696);
            setUpTableMissEntry(dev, L3FWD_TABLE, CONTROLLER);
            setUpTableMissEntry(dev, ENTRY_TABLE, DROP);

        }

    }

    @Override
    public void setUpTableMissEntry(Device device, int table, int op){
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        if (op == DROP) treatment.drop();
        else if (op == CONTROLLER) treatment.punt();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(device.id())
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(TABLE_MISS_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table)
                .build();

        applyRule(flowRule, true);
    }

    @Override
    public void connectTables(Device device, int fromTable, int toTable) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(toTable);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(device.id())
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(TABLE_MISS_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(fromTable)
                .build();

        applyRule(flowRule, true);

    }

    
    private void setStatsTpPorts(Device device, Set<Integer> ports) {

        ports.forEach(port -> {
            setStatsTpPort(device, port);
        });
        
    }

    
    private void setStatsTpPort(Device device, int port){

        setTargetPorts(port);
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                // .matchTcpSrc(TpPort.tpPort(port));
                .matchTcpDst(TpPort.tpPort(port));
        treatment.transition(L3FWD_TABLE);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(device.id())
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(L4STATS_TABLE)
                .build();

        applyRule(flowRule, true);
    }

    @Override
    public void setRequestFlow(Device device) {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        selector.matchEthType(Ethernet.TYPE_IPV4);
        treatment.transition(L4STATS_TABLE);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(device.id())
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(ENTRY_TABLE)
                .build();

        applyRule(flowRule, true);

    }
    
    private void applyRule(FlowRule flowRule, boolean install) {
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        flowOpsBuilder = install ? flowOpsBuilder.add(flowRule) : flowOpsBuilder.remove(flowRule);

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Provisioned vni or forwarding table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.debug("Failed to provision vni or forwarding table");
            }
        }));
    }

    @Override
    public Set<Integer> getTargetPorts() {
        return targetPorts;
    }

    private void setTargetPorts(int port) {
        targetPorts.add(port);
    }

    @Override
    public void addStatsTpPort(int port){
        deviceService.getAvailableDevices().forEach(dev -> setStatsTpPort(dev, port));
    }

    @Override
    public void addStatsTpPorts(Set<Integer> port){
        deviceService.getAvailableDevices().forEach(dev -> setStatsTpPorts(dev, port));
    }
}

/** ref
 * https://github.com/opennetworkinglab/onos/blob/master/pipelines/basic/src/main/java/org/onosproject/pipelines/basic/BasicPipelinerImpl.java
 * https://github.com/opennetworkinglab/onos/search?q=openstack+flow&unscoped_q=openstack+flow
 * https://github.com/opennetworkinglab/onos/blob/master/apps/openstacknetworking/app/src/main/java/org/onosproject/openstacknetworking/impl/OpenstackFlowRuleManager.java
 * https://github.com/opennetworkinglab/onos/blob/master/apps/openstacktelemetry/api/src/main/java/org/onosproject/openstacktelemetry/api/StatsFlowRule.java
 * https://github.com/opennetworkinglab/onos/blob/master/apps/openstacknetworking/api/src/main/java/org/onosproject/openstacknetworking/api/OpenstackFlowRuleService.java
 */
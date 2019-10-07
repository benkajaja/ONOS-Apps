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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
// import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
// import org.onosproject.net.Device;
// import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.statistic.FlowEntryWithLoad;
import org.onosproject.net.statistic.FlowStatisticService;
import org.onosproject.net.statistic.FlowStatisticStore;
import org.onosproject.net.topology.TopologyService;
// import org.onosproject.net.PortNumber;
import org.onosproject.utils.Comparators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Sample Apache Karaf CLI command
 */
@Service
@Command(scope = "onos", name = "d0819",
         description = "Sample Apache Karaf CLI command")
public class AppCommand extends AbstractShellCommand {

    private final Logger log = LoggerFactory.getLogger("jaja.AppComponent");
    private static final int L3FWD_TABLE = 2;
    private static final int DEFAULT_PRIORITY = 10;

    CoreService coreService = get(CoreService.class);
    private ApplicationId appId = coreService.getAppId("org.jaja.d0819");
    
    @Argument(index = 0, name = "action", description = "mac or task",
            required = false)
    String action = null;

    @Override
    protected void doExecute() {
        // AppService appservice = get(AppService.class);
        StatsService statsService = get(StatsService.class);
        OVSPipelineService ovspipelineService = get(OVSPipelineService.class);
        DeviceService deviceService = get(DeviceService.class);
        print("Hello %s", "d0819");
        // List<Device> devices = newArrayList(deviceService.getDevices());
        // Collections.sort(devices, Comparators.ELEMENT_COMPARATOR);
        // List<Port> ports = newArrayList(deviceService.getPorts(d.id()));
        // ports.sort(Comparator.comparing(pas -> pas.number().toLong()));
        if (action.equals("1")) {
            Map<DeviceId, Map<Port, ArrayList<Float>>> rates = statsService.getRate();
            print("[Device/Port] \t %8s\t %8s", "Rx(Mbps)", "Tx(Mbps)");
            List<DeviceId> sortDev = rates.keySet().stream().collect(Collectors.toList());
            Collections.sort(sortDev, Comparators.ELEMENT_ID_COMPARATOR);
            for (DeviceId devices: sortDev){
                print("[Device]: %s", devices.toString());
                Map<Port, ArrayList<Float>> ports = rates.get(devices);
                List<Port> sortPort = ports.keySet().stream().collect(Collectors.toList());
                Collections.sort(sortPort, Comparators.PORT_COMPARATOR);
                for (Port record: sortPort){
                    ArrayList<Float> rate  = ports.get(record);
                    print("\\[Port] %s\t %8.1f\t %8.1f",
                            record.number().toString(),
                            rate.get(0).floatValue()/ (1024 * 1024),
                            rate.get(1).floatValue()/ (1024 * 1024)
                            );
                }
            }
            List<Link> congestLinks = statsService.getCongestLinks();
            print("\n[CongestLinks]");
            int cnt = 0;
            if (congestLinks.isEmpty()) print("No congested link");
            for (Link link : congestLinks){
                print("Link %d: %s -> %s", cnt, link.src().toString(), link.dst().toString());
                cnt ++;
            }

            weigherTest();

        } else if (action.equals("2")){

            print("Target Ports:");
            print(ovspipelineService.getTargetPorts().toString());
            print("Add port 1234");
            ovspipelineService.addStatsTpPort(1234);

        } else if (action.equals("3")){
            
            //selector -> Criteria.class
            //treatment -> instructions.class
            FlowRuleService flowRuleService = get(FlowRuleService.class);
            DeviceService deviceservice = get(DeviceService.class);
            FlowStatisticService flowStatsService = get(FlowStatisticService.class);
            FlowStatisticStore flowStatisticStore = get(FlowStatisticStore.class);
            FlowRuleStore flowRuleStore = get(FlowRuleStore.class);
            float CONGEST_THRESHOLD_MBPS = 1000;
            Set<IpPrefix> toReroute = new HashSet<>();


            List<Device> sortDev = Lists.newArrayList(deviceservice.getAvailableDevices());
            Collections.sort(sortDev, Comparators.ELEMENT_COMPARATOR);

            for (Device dev: sortDev){
                Map<ConnectPoint, List<FlowEntryWithLoad>> m = flowStatsService.loadAllByType(dev, null, Instruction.Type.OUTPUT);
                List<ConnectPoint> sortCP = m.keySet().stream().collect(Collectors.toList());
                Collections.sort(sortCP, Comparators.CONNECT_POINT_COMPARATOR);
                print("\nDev:%s Flow#:%d\t\t\t%15s", dev.id().toString(), flowRuleService.getFlowRuleCount(dev.id()), "Mbps");
                for (ConnectPoint cp: sortCP){
                    List<FlowEntryWithLoad> sortFlow = m.get(cp);
                    // Collections.sort(sortFlow, Comparators.FLOWENTRY_WITHLOAD_COMPARATOR);
                    for(FlowEntryWithLoad f: sortFlow){
                        print("  %s %s\t%15.2f", f.storedFlowEntry().selector().criteria(), f.storedFlowEntry().treatment().allInstructions(), (double) f.load().rate()*8/1024/1024);
                    }
                }
            }
            
            List<Link> congestLinks = statsService.getCongestLinks();
            print("\n[CongestLinks]");
            int cnt = 0;
            if (congestLinks.isEmpty()) {
                print("No congested link");
            }
            for (Link link : congestLinks){
                print("Link %d: %s -> %s", cnt, link.src().toString(), link.dst().toString());
                cnt ++;

                List<FlowEntryWithLoad> m = flowStatsService.loadAllByType(deviceService.getDevice(link.src().deviceId()), link.src().port(), null, Instruction.Type.OUTPUT);
                for(FlowEntryWithLoad f: m){
                    print("  %s %s\t%15.2f", f.storedFlowEntry().selector().criteria(), f.storedFlowEntry().treatment().allInstructions(), (double) f.load().rate()*8/1024/1024);
                    for (Criterion c: Iterables.filter(f.storedFlowEntry().selector().criteria(), ct -> ct.type() == Criterion.Type.IPV4_DST)){
                        IPCriterion cri = (IPCriterion) c;
                        if ((double) f.load().rate()*8/1024/1024 > CONGEST_THRESHOLD_MBPS) toReroute.add(cri.ip());
                    }
                }
            }

            print("\n[Host Need to Reroute]");
            print("%s", toReroute.toString());

            for (IpPrefix ip: toReroute){
                modifyRuleTest(ip, congestLinks.iterator().next());
                toReroute.remove(ip);
            }
            

            /*for (Device dev: sortDev){
                print("\nDev:%s Flow#:%d%15s %15s", dev.id().toString(), flowRuleService.getFlowRuleCount(dev.id()), "bytes#", "packets#");
                for (FlowEntry f: flowRuleService.getFlowEntries(dev.id())){
                    for (Criterion c: Iterables.filter(f.selector().criteria(), ct -> ct.type() == Criterion.Type.TCP_DST)){
                        print(" %s %s\t%15d %15d", f.id().toString(), c.toString(), f.bytes(), f.packets());
                    }
                }
            }*/
            
            /*for (Device dev: sortDev){
                print("\nDev:%s Flow#:%d%15s %15s", dev.id().toString(), flowRuleService.getFlowRuleCount(dev.id()), "bytes#", "packets#");
                for (FlowEntry f: flowRuleService.getFlowEntries(dev.id())){


                    // for (Instruction i: Iterables.filter(f.treatment().allInstructions(), ins -> ins.type() == Instruction.Type.OUTPUT)){
                    //     OutputInstruction in = (OutputInstruction) i;
                    //     if (in.port() == PortNumber.CONTROLLER) continue;
                    //     print("  %s %s", f.selector().criteria(), in.port());
                    //     // print("%s", i instanceof OutputInstruction);
                    // }
                    for (Criterion c: Iterables.filter(f.selector().criteria(), ct -> ct.type() == Criterion.Type.IPV4_DST)){
                        IPCriterion cri = (IPCriterion) c;
                        print("  %s %s\t%15d %15d", cri.ip(), f.treatment().allInstructions(), f.bytes(), f.packets());
                        // print(" %s %s\t%15d %15d", f.id().toString(), c.toString(), f.bytes(), f.packets());
                    }
                    

                }
            }*/





            // https://github.com/opennetworkinglab/onos/blob/master/core/api/src/main/java/org/onosproject/net/statistic/FlowStatisticStore.java
            // https://github.com/opennetworkinglab/onos/blob/123f0e08ad42cd36ed9e9106d4be7e3b66191bb3/apps/imr/app/src/main/java/org/onosproject/imr/IntentMonitorAndRerouteManager.java

            // https://github.com/opennetworkinglab/onos/blob/master/core/net/src/main/java/org/onosproject/net/statistic/impl/StatisticManager.java
            // https://github.com/opennetworkinglab/onos/blob/master/core/common/src/test/java/org/onosproject/store/trivial/SimpleStatisticStore.java
            // https://github.com/opennetworkinglab/onos/blob/master/core/net/src/main/java/org/onosproject/net/statistic/impl/FlowStatisticManager.java

            // https://github.com/opennetworkinglab/onos/blob/master/core/api/src/main/java/org/onosproject/net/flow/FlowRuleStore.java
            // https://github.com/opennetworkinglab/onos/blob/master/core/common/src/test/java/org/onosproject/store/trivial/SimpleFlowRuleStore.java
            // https://github.com/opennetworkinglab/onos/blob/master/core/net/src/main/java/org/onosproject/net/flow/impl/FlowRuleManager.java
        }
        
    }

    private void weigherTest(){
        StatsService statsService = get(StatsService.class);
        TopologyService topologyService = get(TopologyService.class);
        int cnt = 0;
        // Host src = hostService.getHost(HostId.hostId(MacAddress.valueOf("00:00:00:00:00:01")));
        // Host dst = hostService.getHost(HostId.hostId(MacAddress.valueOf("00:00:00:00:00:04")));
        String src = "of:0000000000000001";
        String dst = "of:0000000000000004";

        Set<Path> paths =
                    topologyService.getPaths(topologyService.currentTopology(),
                                             DeviceId.deviceId(src),
                                             DeviceId.deviceId(dst),
                                             new TopoWeigher(statsService.getCongestLinks()));
        print("\n[Possible Path] %s -> %s", src, dst);
        for (Path path: paths){
            print("\\Path %d   weight:%s", cnt, path.weight());
            for (Link link: path.links()){
                print(" %s -> %s", link.src().toString(), link.dst().toString());
            }
            cnt ++;
        }

        cnt = 0;
        dst = "of:0000000000000001";
        src = "of:0000000000000004";

        paths = topologyService.getPaths(topologyService.currentTopology(),
                                            DeviceId.deviceId(src),
                                            DeviceId.deviceId(dst),
                                            new TopoWeigher(statsService.getCongestLinks()));
        print("\n[Possible Path] %s -> %s", src, dst);
        for (Path path: paths){
            print("\\Path %d   weight:%s", cnt, path.weight());
            for (Link link: path.links()){
                print(" %s -> %s", link.src().toString(), link.dst().toString());
            }
            cnt ++;
        }

    }

    private void modifyRuleTest(IpPrefix ip, Link link) {
        StatsService statsService = get(StatsService.class);
        TopologyService topologyService = get(TopologyService.class);
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(ip);

        Set<Path> paths = paths = topologyService.getKShortestPaths(topologyService.currentTopology(),
                            link.src().deviceId(),
                            link.dst().deviceId(),
                            new TopoWeigher(statsService.getCongestLinks()), 1);

        treatment.setOutput(paths.iterator().next().src().port());
        print("Change to port %s", paths.iterator().next().src().port());

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(link.src().deviceId())
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(L3FWD_TABLE)
                .build();

        applyRule(flowRule, true);

    }
    private void applyRule(FlowRule flowRule, boolean install) {
        FlowRuleService flowRuleService = get(FlowRuleService.class);
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        flowOpsBuilder = install ? flowOpsBuilder.modify(flowRule) : flowOpsBuilder.remove(flowRule);

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

}


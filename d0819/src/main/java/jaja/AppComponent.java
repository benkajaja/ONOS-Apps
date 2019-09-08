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

import java.util.List;
// import java.util.Map;
import java.util.Set;

// import com.google.common.collect.Maps;

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
// import org.onlab.packet.MacAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
// import org.onosproject.net.Device;
// import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.host.HostService;
// import org.onosproject.net.host.HostEvent;
import org.onosproject.net.link.LinkEvent;
// import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.PathService;
// import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEvent;
// import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
// import org.onosproject.net.topology.TopologyVertex;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Multipath Reroute Base On L3
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private ApplicationId appId;
    private TopologyListener topologyListener = new InternalTopologyListener();
    private DeviceListener deviceListener = new InternalDeviceListener();
    private static final int DEFAULT_PRIORITY = 10;
    private static final int DEFAULT_DURATION = 20;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected AppService appservice;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OVSPipelineService ovspipelineService;

    @Activate
    protected void activate() {
        // cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.jaja.d0819");
        packetService.addProcessor(processor, PacketProcessor.director(0)); 

        appservice.initializeAppService(log, deviceService, linkService, topologyService);
        appservice.initializeStatsTask();
        appservice.startTask();

        ovspipelineService.initializeService(appId);

        requestIntercepts();

        topologyService.addListener(topologyListener);
        deviceService.addListener(deviceListener);

        weigherTest();
        multiTableTest();
                                    
        log.info("log name: {}", log.getName());
        log.info("appid: {} {}", appId.id(), appId.name());
        log.info("Hello");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        withdrawIntercepts();
        topologyService.removeListener(topologyListener);
        deviceService.removeListener(deviceListener);
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(processor);
        processor = null;
        appservice.stopTask();
        log.info("Get out!");
    }

    private void weigherTest(){
        int cnt = 0;
        // Host src = hostService.getHost(HostId.hostId(MacAddress.valueOf("00:00:00:00:00:01")));
        // Host dst = hostService.getHost(HostId.hostId(MacAddress.valueOf("00:00:00:00:00:04")));
        
        String src = "of:0000000000000001";
        String dst = "of:0000000000000004";
        
        Set<Path> paths =
                    topologyService.getPaths(topologyService.currentTopology(),
                                            //  src.location().deviceId(),
                                            //  dst.location().deviceId(),
                                            DeviceId.deviceId(src),
                                            DeviceId.deviceId(dst),
                                             new TopoWeigher(appservice.getCongestLinks()));
        for (Path path: paths){
            log.info("[Path {}]   weight:{}", cnt, path.weight());
            for (Link link: path.links()){
                log.info("{} -> {}", link.src(), link.dst());
            }
            cnt ++;
        }
    }

    private void multiTableTest(){

        String src = "of:0000000000000001";

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                                           .matchEthType(Ethernet.TYPE_IPV4)
                                           .matchIPProtocol(IPv4.PROTOCOL_TCP)
                                           .matchTcpSrc(TpPort.tpPort(8787));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                                             .drop();



        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(DeviceId.deviceId(src))
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                // .makeTemporary(DEFAULT_DURATION)
                .forTable(1)
                .build();

        flowRuleService.applyFlowRules(rule);


        selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4);

        treatment = DefaultTrafficTreatment.builder()
                    .transition(1);

        rule = DefaultFlowRule.builder()
                    .forDevice(DeviceId.deviceId(src))
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(0)
                    .fromApp(appId)
                    .makePermanent()
                    // .makeTemporary(DEFAULT_DURATION)
                    .forTable(0)
                    .build();

        flowRuleService.applyFlowRules(rule);

    }

    /**
     * Request packet in via packet service.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);

        // selector.matchEthType(Ethernet.TYPE_IPV6);
        // if (ipv6Forwarding) {
        //     packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
        // } else {
        //     packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        // }
    }

    /**
     * Cancel request for packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        selector.matchEthType(Ethernet.TYPE_IPV6);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    private class InternalTopologyListener implements TopologyListener {
        
        @Override
        public void event(TopologyEvent event) {
            List<Event> reasons = event.reasons();
            if (reasons != null) {
                reasons.forEach(re -> {
                    if (re instanceof LinkEvent) {
                        LinkEvent le = (LinkEvent) re;
                        log.info("[TopoEvent] AType:{}", le.type());
                        log.info("[TopoEvent] BType:{}", re.type());
                        /*if (le .type() == LinkEvent.Type.LINK_REMOVED) {
                            fixBlackhole( le.subject().src());
                        }*/
                        re.type().equals(TopologyEvent.Type.TOPOLOGY_CHANGED);
                        re.type().equals(TopologyEvent.Type.TOPOLOGY_CHANGED);
                        
                    } else log.info("[TopoEvent] not link event Class:{}", re.getClass().toString());

                });
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            // log.info("[DevEvent] Type:{}", event.type());
            // Device device = event.subject();
        }
    }

    private class ReactivePacketProcessor implements PacketProcessor {
        
        @Override
        public void process(PacketContext context) {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            
            

            if (context.isHandled()) {
                // log.info("[DEBUG] packet is handled!");
                return;
            }
            
            if (ethPkt == null) {
                // log.info("[DEBUG] packet is not an Ethernet frame!");
                return;
            }

            if (isControlPacket(ethPkt)) {
                // log.info("[DEBUG] control packet!");
                return;
            }

            debugmessage(context);
            
            HostId id = HostId.hostId(ethPkt.getDestinationMAC());

            // Do we know who this is for? If not, flood and bail.
            Host dst = hostService.getHost(id);
            if (dst == null) {
                flood(context);
                return;
            }
            log.info("[DEBUG] know the host mac:{}, location:{}", dst.mac(), dst.location());

            // Are we on an edge switch that our destination is on? If so,
            // simply forward out to the destination and bail.
            if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
                if (!pkt.receivedFrom().port().equals(dst.location().port())) {
                    // log.info("[DEBUG] At Edge switch");
                    installRule(context, dst.location().port());
                    // packetOut(context, PortNumber.TABLE);
                    packetOut(context, dst.location().port());
                }
                return;
            }

            // Otherwise, get a set of paths that lead from here to the
            // destination edge switch.
            Set<Path> paths =
                    topologyService.getPaths(topologyService.currentTopology(),
                                             pkt.receivedFrom().deviceId(),
                                             dst.location().deviceId());
            if (paths.isEmpty()) {
                // If there are no paths, flood and bail.
                log.info("[DEBUG] path is empty");
                flood(context);
                return;
            }
            // log.info("[DEBUG] path is not empty");

            // Otherwise, pick a path that does not lead back to where we
            // came from; if no such path, flood and bail.
            Path path = pickForwardPathIfPossible(paths, pkt.receivedFrom().port());
            if (path == null) {
                log.warn("Don't know where to go from here {} for {} -> {}",
                         pkt.receivedFrom(), ethPkt.getSourceMAC(), ethPkt.getDestinationMAC());
                flood(context);
                return;
            }

            // Otherwise forward and be done with it.
            installRule(context, path.src().port());
            // packetOut(context, PortNumber.TABLE);
            packetOut(context,  path.src().port());
            
        }

        private void debugmessage(PacketContext context) {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            // IPacket ipPkt = ethPkt.getPayload();

            

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 p = (IPv4) ethPkt.getPayload();
                p.getPayload().getPayload().getPayload();

                log.info("[IPv4] DeviceID {} {} --> {}, {} --> {}", 
                        pkt.receivedFrom().toString(), 
                        ethPkt.getSourceMAC(),
                        ethPkt.getDestinationMAC(),
                        IPv4.fromIPv4Address(p.getSourceAddress()),
                        IPv4.fromIPv4Address(p.getDestinationAddress())
                        );

            } else if (ethPkt.getEtherType() == Ethernet.TYPE_ARP){
                ARP p = (ARP) ethPkt.getPayload();
                log.info("[ARP]{}  DeviceID {} {} --> {}, {} --> {}",
                        (p.getOpCode() == ARP.OP_REPLY)? "[REPLY]": "[REQUEST]",
                        pkt.receivedFrom().toString(), 
                        ethPkt.getSourceMAC(),
                        ethPkt.getDestinationMAC(),
                        IPv4.fromIPv4Address(IPv4.toIPv4Address(p.getSenderProtocolAddress())),
                        IPv4.fromIPv4Address(IPv4.toIPv4Address(p.getTargetProtocolAddress()))
                        );
            }

        }

        private boolean isControlPacket(Ethernet eth ) {
            short type = eth.getEtherType();
            return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
        }
        
        private Path pickForwardPathIfPossible(Set<Path> paths, PortNumber notToPort) {
            for (Path path : paths) {
                if (!path.src().port().equals(notToPort)) {
                    return path;
                }
            }
            return null;
        }

        private void packetOut(PacketContext context, PortNumber portNumber) {
            context.treatmentBuilder().setOutput(portNumber);
            context.send();
        }
        
        private void flood(PacketContext context){
            // packetOut(context, PortNumber.FLOOD);
            if (topologyService.isBroadcastPoint(topologyService .currentTopology(),
                                         context.inPacket().receivedFrom())) {
                packetOut(context, PortNumber.FLOOD);
                log.warn("[Flood] DeviceID {}", context.inPacket().receivedFrom().deviceId());
            } else {
                context.block();
                // log.warn("[Flood Ban] DeviceID {}", context.inPacket().receivedFrom().deviceId());
            }
        }

        private void installRule(PacketContext context, PortNumber outport) {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            
            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

            if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {
                log.info("[DEBUG] Directly pass arp packet to {}", outport);
                packetOut(context, outport);
                return;
            }

            // log.info("[DEBUG] Not arp packet", outport);

            IPv4 p = (IPv4) ethPkt.getPayload();

            selectorBuilder//.matchInPort(pkt.receivedFrom().port())
                    // .matchEthSrc(ethPkt.getSourceMAC())
                    // .matchEthDst(ethPkt.getDestinationMAC())
                    // .matchIPSrc(IpPrefix.valueOf(p.getSourceAddress(), IpPrefix.MAX_INET_MASK_LENGTH))
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(IpPrefix.valueOf(p.getDestinationAddress(), IpPrefix.MAX_INET_MASK_LENGTH))
                    
                    ;

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(outport)
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatment)
                    .withPriority(DEFAULT_PRIORITY)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(DEFAULT_DURATION)
                    .add();

            flowObjectiveService.forward(pkt.receivedFrom().deviceId(), forwardingObjective);

            log.info("[ADD RULE]  DeviceID {} Match {} OutPort {}", 
                        pkt.receivedFrom().deviceId(), 
                        // ethPkt.getSourceMAC(), 
                        // ethPkt.getDestinationMAC(), 
                        IpPrefix.valueOf(p.getDestinationAddress(), IpPrefix.MAX_INET_MASK_LENGTH).toString(),
                        outport);
            /*log.info("[DEBUG] {} {}", 
                    IPv4.fromIPv4Address(p.getDestinationAddress()),
                    IpPrefix.valueOf(p.getDestinationAddress(), IpPrefix.MAX_INET_MASK_LENGTH).contains(IpAddress.valueOf(p.getDestinationAddress()))
            );*/
        }

        /** ref
         * https://github.com/MaoJianwei/ONOS_OVS_Manager_Bootcamp2016/blob/master/ovsmanager/OVSPipeline.java
         * https://github.com/opennetworkinglab/onos-app-samples/blob/master/tvue/src/main/java/org/onosproject/tvue/TopologyResource.java
         * https://www.twblogs.net/a/5b8a41992b71775d1ce63797
         * https://www.twblogs.net/a/5b8a41a02b71775d1ce637b5
         * https://www.twblogs.net/a/5b8a41a52b71775d1ce637d7
         * https://medium.com/@fdgkhdkgh/tracecode-org-onosproject-fwd-4fbc4297ecca
         * https://github.com/opennetworkinglab/onos-app-samples/blob/master/sdx-l3/src/main/java/org/onosproject/sdxl3/impl/SdxL3PeerManager.java
         * https://studiofreya.com/java/how-to-sort-a-set-in-java-example/
         * 
         * 
         * https://github.com/opennetworkinglab/onos/blob/021d2eb175b8e46d4690cd9e1243301ddd903bcc/apps/pathpainter/src/main/java/org/onosproject/pathpainter/PathPainterTopovMessageHandler.java
         * https://github.com/opennetworkinglab/onos/blob/021d2eb175b8e46d4690cd9e1243301ddd903bcc/core/api/src/main/java/org/onosproject/net/topology/AbstractPathService.java
         * https://github.com/opennetworkinglab/onos/blob/123f0e08ad42cd36ed9e9106d4be7e3b66191bb3/core/net/src/main/java/org/onosproject/net/intent/impl/compiler/ConnectivityIntentCompiler.java
         * https://github.com/opennetworkinglab/onos/blob/bd508ede5c98ae0c811d765f38e79cd9abef8000/apps/fwd/src/main/java/org/onosproject/fwd/ReactiveForwarding.java
         * https://github.com/opennetworkinglab/onos/blob/aeecd041f6ec78e7ecdf23bf2b73301f35700523/apps/segmentrouting/app/src/main/java/org/onosproject/segmentrouting/mcast/McastHandler.java
         * https://github.com/opennetworkinglab/onos/blob/cc0012423b77cd2f4a77ecd10eecce25e28ed06f/apps/pce/app/src/test/java/org/onosproject/pce/pceservice/PceManagerTest.java
         * https://github.com/opennetworkinglab/onos/blob/cc0012423b77cd2f4a77ecd10eecce25e28ed06f/core/net/src/test/java/org/onosproject/net/topology/impl/TopologyManagerTest.java
         * https://github.com/opennetworkinglab/onos/blob/021d2eb175b8e46d4690cd9e1243301ddd903bcc/core/common/src/main/java/org/onosproject/common/DefaultTopology.java
         * https://github.com/opennetworkinglab/onos-app-samples/blob/master/carrierethernet/src/main/java/org/onosproject/ecord/carrierethernet/app/CarrierEthernetSpanningTreeWeight.java
         * https://github.com/opennetworkinglab/onos/blob/master/core/api/src/main/java/org/onosproject/net/topology/HopCountLinkWeigher.java
         * 
         * https://github.com/opennetworkinglab/onos-app-samples/blob/master/flowtest/src/main/java/org/onosproject/flowruletest/dispatch/FlowRuleTest.java
         * https://github.com/MaoJianwei/ONOS_OVS_Manager_Bootcamp2016/blob/master/ovsmanager/OVSPipeline.java
         * https://www.maojianwei.com/2016/04/25/Share-in-SDN-Battle-Group-ONOS-in-practice-in-Bootcamp-2016-OVS-Manager/
         * 
         * https://github.com/opennetworkinglab/onos/blob/onos-2.0/apps/ofagent/src/main/java/org/onosproject/ofagent/impl/DefaultOFSwitch.java
         * https://wiki.onosproject.org/display/ONOS/Flow+Rule+Subsystem
         */
    }
}

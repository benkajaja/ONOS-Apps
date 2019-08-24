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
import com.google.common.collect.Maps;
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
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.DeviceId;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import java.lang.System;
import java.util.Optional;
import java.util.Map;

/**
 * MAC learning
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Some configurable property. */
    // private String someProperty;
    private ApplicationId appId;
    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    protected Map<DeviceId, Map<MacAddress, PortNumber>> macTables = Maps.newConcurrentMap();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Activate
    protected void activate() {
        // cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.jaja.maclearning");
        packetService.addProcessor(processor, PacketProcessor.director(0));
        
        packetService.requestPackets(DefaultTrafficSelector.builder()
                                    .matchEthType(Ethernet.TYPE_IPV4).build(), 
                                    PacketPriority.REACTIVE, 
                                    appId, 
                                    Optional.empty());


        log.info("log name: {}", log.getName());
        log.info("appid: {} {}", appId.id(), appId.name());
        log.info("Hello");
        System.out.println("test");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(processor);
        processor = null;
        
        packetService.cancelPackets(DefaultTrafficSelector.builder()
                                    .matchEthType(Ethernet.TYPE_IPV4).build(), 
                                    PacketPriority.REACTIVE, 
                                    appId);
        log.info("Get Out");
    }

    private class ReactivePacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            InboundPacket pkt = context.inPacket();
            System.out.println("hahaha");
            Ethernet ethPkt = pkt.parsed();
            macTables.putIfAbsent(pkt.receivedFrom().deviceId(), Maps.newConcurrentMap());

            /*if (context.isHandled()) {
                // log.info("[DEBUG] packet is handled!");
                return;
            }*/

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) 
                log.info("[IPv4] DeviceID {} from {} to {}", 
                        pkt.receivedFrom().toString(), 
                        ethPkt.getSourceMAC(),
                        ethPkt.getDestinationMAC());
            else if (ethPkt.getEtherType() == Ethernet.TYPE_ARP){
                ARP p = (ARP) ethPkt.getPayload();
                log.info("[ARP]{}  DeviceID {} from {} to {}",
                        (p.getOpCode() == ARP.OP_REPLY)? "[REPLY]": "[REQUEST]",
                        pkt.receivedFrom().toString(), 
                        ethPkt.getSourceMAC(),
                        ethPkt.getDestinationMAC());
            }
            else return;
            
            macLearning(context);
            
        }

        
        private void macLearning(PacketContext context){
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            MacAddress src = ethPkt.getSourceMAC();
            MacAddress dst = ethPkt.getDestinationMAC();

            if(src == null || dst == null){
                log.info("[DEBUG] no src or dst mac address!");
                return;
            }

            ConnectPoint cp = pkt.receivedFrom();
            Map<MacAddress, PortNumber> macTable = macTables.get(cp.deviceId());
            macTable.put(src, cp.port());
            PortNumber outPort = macTable.get(dst);

            if (outPort != null){

                selectorBuilder.matchEthDst(dst)
                               .matchEthSrc(src)
                            //    .matchInPort(cp.port())
                               ;  
            
                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                            .setOutput(outPort)
                            .build();
        
                ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                            .withSelector(selectorBuilder.build())
                            .withTreatment(treatment)
                            .withPriority(PacketPriority.HIGH.priorityValue())
                            .withFlag(ForwardingObjective.Flag.VERSATILE)
                            .fromApp(appId)
                            .makeTemporary(20) //timeout
                            .add();

                flowObjectiveService.forward(cp.deviceId(), forwardingObjective);

                packetOut(context, PortNumber.TABLE);

                log.info("[ADD RULE]  DeviceID {} Match {} {} OutPort {}", 
                    cp.deviceId(), 
                    src,
                    dst, 
                    outPort);

            } else {
                packetOut(context, PortNumber.FLOOD);
                log.warn("[Flood] DeviceID {}", cp.deviceId());
            }
        }

        private void packetOut(PacketContext context, PortNumber portNumber) {
            context.treatmentBuilder().setOutput(portNumber);
            context.send();
        }
    }
}
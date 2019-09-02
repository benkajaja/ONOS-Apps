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
import org.onosproject.cli.AbstractShellCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
// import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.onosproject.net.Device;
// import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.topology.TopologyService;
// import org.onosproject.net.PortNumber;
import org.onosproject.utils.Comparators;


/**
 * Sample Apache Karaf CLI command
 */
@Service
@Command(scope = "onos", name = "d0819",
         description = "Sample Apache Karaf CLI command")
public class AppCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "action", description = "mac or task",
            required = false)
    String action = null;

    @Override
    protected void doExecute() {
        AppService appservice = get(AppService.class);
        print("Hello %s", "d0819");
        // List<Device> devices = newArrayList(deviceService.getDevices());
        // Collections.sort(devices, Comparators.ELEMENT_COMPARATOR);
        // List<Port> ports = newArrayList(deviceService.getPorts(d.id()));
        // ports.sort(Comparator.comparing(pas -> pas.number().toLong()));

        Map<DeviceId, Map<Port, ArrayList<Float>>> rates = appservice.getRate();
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
        List<Link> congestLinks = appservice.getCongestLinks();
        print("\n[CongestLinks]");
        int cnt = 0;
        if (congestLinks.isEmpty()) print("No congested link");
        for (Link link : congestLinks){
            print("Link %d: %s -> %s", cnt, link.src().toString(), link.dst().toString());
            cnt ++;
        }

        weigherTest(appservice);


        /*Map<DeviceId, Map<Port, ArrayList<Long>>> records = appservice.getRecord();
        print("[Device/Port] \t %8s\t %8s\t %8s\t %8s", "RxB(MB)", "TxB(MB)", "RxP", "TxP");
        for(Map.Entry<DeviceId, Map<Port, ArrayList<Long>>>  devices : records.entrySet()){
            Map<Port, ArrayList<Long>> ports = devices.getValue();
            print("[Device]: %s",devices.getKey().toString());
            for(Map.Entry<Port, ArrayList<Long>>  record : ports.entrySet()){
                print("\\[Port] %s\t %8.1f\t %8.1f\t %8d\t %8d",
                        record.getKey().number().toString(),
                        record.getValue().get(0).doubleValue()/ (1024 * 1024),
                        record.getValue().get(1).doubleValue()/ (1024 * 1024),
                        record.getValue().get(2),
                        record.getValue().get(3));
            }
        }*/

    
        /*if (action.equals("1")){
            Map<DeviceId, Map<MacAddress, PortNumber>> macTables = appservice.getMacTable();
            
            print("Size: %d", macTables.size());

            for(Map.Entry<DeviceId, Map<MacAddress, PortNumber>>  macTable : macTables.entrySet()){
                DeviceId dev = macTable.getKey();
                Map<MacAddress, PortNumber> pairs = macTable.getValue();
                print("DeviceID: %s",dev);
                for (Map.Entry<MacAddress, PortNumber> pair : pairs.entrySet()){
                    MacAddress mac = pair.getKey();
                    PortNumber port = pair.getValue();
                    print("  MAC: %s  Port: %s", mac, port);
                }
            }

        } else if (action.equals("2")){
            Map<DeviceId, Map<Port, ArrayList<Long>>> records = appservice.getRecord();
            print("[Device/Port] \t %8s\t %8s\t %8s\t %8s", "RxB(MB)", "TxB(MB)", "RxP", "TxP");
            for(Map.Entry<DeviceId, Map<Port, ArrayList<Long>>>  devices : records.entrySet()){
                Map<Port, ArrayList<Long>> ports = devices.getValue();
                print("[Device]: %s",devices.getKey().toString());
                for(Map.Entry<Port, ArrayList<Long>>  record : ports.entrySet()){
                    print("\\[Port] %s\t %8.1f\t %8.1f\t %8d\t %8d",
                            record.getKey().number().toString(),
                            record.getValue().get(0).doubleValue()/ (1024 * 1024),
                            record.getValue().get(1).doubleValue()/ (1024 * 1024),
                            record.getValue().get(2),
                            record.getValue().get(3));
                }
            }

            // portStatsReaderTask task : statsTasks.values()

            // Map<ConnectPoint, ArrayList<Long>> records = appservice.getRecord();
            
            // for(Map.Entry<ConnectPoint, ArrayList<Long>>  record : records.entrySet()){
            //     print("[Device/Port] %s/%s %d\t %d\t %d\t %d",
            //             record.getKey().deviceId().toString(),
            //             record.getKey().port().toString(),
            //             record.getValue().get(0),
            //             record.getValue().get(1),
            //             record.getValue().get(2),
            //             record.getValue().get(3));
            // }

            // // portStatsReaderTask task : statsTasks.values()

        }else{
            print("Command doesn't match");
        }*/
        

        
    }

    private void weigherTest(AppService appservice){
        TopologyService topologyService = appservice.getTopologyService();
        int cnt = 0;
        // Host src = hostService.getHost(HostId.hostId(MacAddress.valueOf("00:00:00:00:00:01")));
        // Host dst = hostService.getHost(HostId.hostId(MacAddress.valueOf("00:00:00:00:00:04")));
        String src = "of:0000000000000001";
        String dst = "of:0000000000000004";

        Set<Path> paths =
                    topologyService.getPaths(topologyService.currentTopology(),
                                             DeviceId.deviceId(src),
                                             DeviceId.deviceId(dst),
                                             new TopoWeigher(appservice.getCongestLinks()));
        print("\n[Possible Path] %s -> %s", src, dst);
        for (Path path: paths){
            print("\\Path %d   weight:%s", cnt, path.weight());
            for (Link link: path.links()){
                print(" %s -> %s", link.src().toString(), link.dst().toString());
            }
            cnt ++;
        }
    }

}


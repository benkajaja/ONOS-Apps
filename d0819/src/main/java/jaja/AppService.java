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
import java.util.Map;

import org.onlab.packet.MacAddress;
import org.onosproject.core.Application;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Skeletal ONOS application API.
 */
public interface AppService {

    void initializeAppService(Application appId);

    Map<DeviceId, Map<MacAddress, PortNumber>> getMacTable();
    
    void clearMacTable();

    void initializeMacTable();

}

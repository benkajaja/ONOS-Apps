package jaja;

// import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;

import java.util.List;

import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;

public class TopoWeigher implements LinkWeigher {

    protected TopologyService topologyService;
    private List<Link> congestLinks;
    private static final ScalarWeight ZERO = new ScalarWeight(0.0);
    private static final ScalarWeight ONE = new ScalarWeight(1.0);

    public TopoWeigher(List<Link> congestLinks) {
        this.congestLinks = congestLinks;
    }

    @Override
    public Weight weight(TopologyEdge edge) {
        /*if (edge.link().state() == ACTIVE) {
            return edge.link().type() == INDIRECT ? indirectLinkCost : ONE;
        } else {
            return getNonViableWeight();
        }*/

        // if (edge.link().src().deviceId().equals(DeviceId.deviceId("of:0000000000000001")) &&
        //     edge.link().dst().deviceId().equals(DeviceId.deviceId("of:0000000000000004")) ){
        //     return ScalarWeight.NON_VIABLE_WEIGHT;
        // } else return ONE;

        if (congestLinks.contains(edge.link())){
            return getNonViableWeight();
        } else return ONE;

    }

    @Override
    public Weight getInitialWeight() {
        return ZERO;
    }

    @Override
    public Weight getNonViableWeight() {
        return ScalarWeight.NON_VIABLE_WEIGHT;
    }

}
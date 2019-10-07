package jaja;

import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;



/**
 * Created by kspviswa-onos on 15/8/16.
 * Modified by jaja on 19/7/31.
 */
public class portStatsReaderTask {

    private long delay;
    private Timer timer = new Timer();
    private Logger log;
    private Device device;
    private boolean exit;
    private PortNumber port;
    private PortStatistics portStats;
    protected DeviceService deviceService;
    protected LinkService linkService;
    private ArrayList<Long> statsValue;
    private ArrayList<Float> statsRate;
    private List<Link> congestLinks;
    // private float txRate;
    // private float rxRate;

    private boolean SHOW_STATS_LOG = false;
    private short statsType = DELTA;
    public static final short DELTA = 0;
    public static final short CUMULATIVE = 1;
    private float CONGEST_THRESHOLD_MBPS = 1000;
    public static final short INGRESS = 0;
    public static final short EGRESS = 1;
    

    class Task extends TimerTask {

        public Device getDevice() {
            return device;
        }

        public long getDelay() {
            return delay;
        }

        @Override
        public void run() {
            while (!isExit()) {                
                
                PortStatistics portdeltastat = deviceService.getDeltaStatisticsForPort(getDevice().id(), getPort());
                PortStatistics portstat = deviceService.getStatisticsForPort(getDevice().id(), getPort());
                
                if (SHOW_STATS_LOG){
                    log.info("[DeviceID] {}", getDevice().id());
                    double rate = (portdeltastat.bytesReceived() / (1024 * 1024));
                    log.info("Port " + port + " Received " + portstat.bytesReceived() + " bytes");
                    log.info("Port " + port + " Received " + portstat.packetsReceived() + " packets");
                    log.info("Port " + port + " Rate " + rate + " MB/s");
                }
                
                if (statsType == DELTA) updateStats(portdeltastat);
                else if (statsType == CUMULATIVE) updateStats(portstat);
                updateRates(portdeltastat);

                Set<Link> inlinks = linkService.getIngressLinks(new ConnectPoint(getDevice().id(), getPort()));
                for (Link link : inlinks){
                    if (isCongest(portdeltastat, INGRESS)){
                        log.info("[DEBUG] congest detected! {} -> {}", link.src(), link.dst());
                        if(!congestLinks.contains(link)){
                            congestLinks.add(link);
                        }
                    }
                    else congestLinks.remove(link);
                }

                Set<Link> elinks = linkService.getEgressLinks(new ConnectPoint(getDevice().id(), getPort()));
                for (Link link : elinks){
                    if (isCongest(portdeltastat, EGRESS)){
                        log.info("[DEBUG] congest detected! {} -> {}", link.src(), link.dst());
                        if(!congestLinks.contains(link)){
                            congestLinks.add(link);
                        }
                    }
                    else congestLinks.remove(link);
                }

                try {
                    Thread.sleep((getDelay() * 1000));
                } catch (InterruptedException e) {
                    log.error("exception!");
                    e.printStackTrace();
                }
            }
        }
    }

    public void schedule() {
        this.getTimer().schedule(new Task(), 0, 1000);
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public PortStatistics getPortStats() {
        return portStats;
    }

    public void setPortStats(PortStatistics portStats) {
        this.portStats = portStats;
    }

    public PortNumber getPort() {
        return port;
    }

    public void setPort(PortNumber port) {
        this.port = port;
    }

    public DeviceService getDeviceService() {
        return deviceService;
    }

    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public void updateStats(PortStatistics ps){
        long RxB = ps.bytesReceived();
        long TxB = ps.bytesSent();
        long RxP = ps.packetsReceived();
        long TxP = ps.packetsSent();

        statsValue.set(0, Long.valueOf(RxB));
        statsValue.set(1, Long.valueOf(TxB));
        statsValue.set(2, Long.valueOf(RxP));
        statsValue.set(3, Long.valueOf(TxP));
    }

    public void updateRates(PortStatistics ps){

        long RxB = ps.bytesReceived();
        long TxB = ps.bytesSent();
        float duration = ((float) ps.durationSec()) +
                         (((float) ps.durationNano()) / TimeUnit.SECONDS.toNanos(1)); 

        statsRate.set(0, (float) RxB * 8 / duration);
        statsRate.set(1, (float) TxB * 8 / duration);
        // log.info("[DEBUG] rates {} {} ", (float) RxB * 8 / duration, (float) RxB * 8 / duration);

    }

    public void setRecords(ArrayList<Long> record){
        this.statsValue = record;
        // log.info("[DEBUG] record size {}", record.size());
    }

    public void setRates(ArrayList<Float> rate){
        this.statsRate = rate;
        // log.info("[DEBUG] rate size {}", rate.size());
    }

    public void setShowStatsLog(boolean show){
        this.SHOW_STATS_LOG = show;
    }

    public void setStatsType(short type){
        this.statsType = type;
    }

    public void setCongestLinks(List<Link> congestLinks){
        this.congestLinks = congestLinks;
    }

    public boolean isCongest(PortStatistics ps, short type){
        float duration = ((float) ps.durationSec()) +
                         (((float) ps.durationNano()) / TimeUnit.SECONDS.toNanos(1)); 

        if (type == INGRESS){
            float rxRate = (float) ps.bytesReceived() * 8 / duration / (1024 * 1024);
            return (rxRate > CONGEST_THRESHOLD_MBPS)? true : false;
        }
        else if (type == EGRESS){
            float txRate = (float) ps.bytesSent() * 8 / duration / (1024 * 1024);
            return (txRate > CONGEST_THRESHOLD_MBPS)? true : false;
        }
        else return false;

    }

    public void setLinkService(LinkService linkService) {
        this.linkService = linkService;
    }
}

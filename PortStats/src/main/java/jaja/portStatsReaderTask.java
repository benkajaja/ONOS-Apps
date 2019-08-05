package jaja;

import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class portStatsReaderTask {

    private long delay;
    private Timer timer = new Timer();
    private Logger log;
    private Device device;
    private boolean exit;
    private PortNumber port;
    private PortStatistics portStats;
    protected DeviceService deviceService;

    class Task extends TimerTask {

        public Device getDevice() {
            return device;
        }
        public DeviceService getDeviceService() {
            return deviceService;
        }
        public long getDelay() {
            return delay;
        }

        @Override
        public void run() {
            while (!isExit()) {                
                log.info("[DeviceID] {}", getDevice().id());
                PortStatistics portstat = deviceService.getStatisticsForPort(getDevice().id(), getPort());
                log.info("Port {} Received {} bytes",   port, portstat.bytesReceived());
                log.info("Port {} Received {} packets", port, portstat.packetsReceived());
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
}

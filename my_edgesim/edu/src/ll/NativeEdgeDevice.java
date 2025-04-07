package ll;

public class NativeEdgeDevice extends EdgeDevice{
    Scheduler scheduler;

    public NativeEdgeDevice(Scheduler scheduler, int deviceId, double mips, double storage, double latitude, double longitude, double wlan_id, double attractiveness, double downloadspeed, double uploadspeed) {
        super(deviceId, mips, storage, latitude, longitude, wlan_id, attractiveness, downloadspeed, uploadspeed);
        this.scheduler = scheduler;
    }


}


package ll;

public class MobileDevice {
    private APP app;
    private double mobiledevice_latitude;          // 纬度
    private double mobiledevice_longitude;
    private int mobiledevice_attractiveness;
    private int connectionType; //0:lan,1:wlan
    private int deviceId;

    public MobileDevice(APP app, double latitude, double longitude, int attractiveness, int connectionType, int deviceId) {
        this.app = app;
        this.mobiledevice_latitude = latitude;
        this.mobiledevice_longitude = longitude;
        this.mobiledevice_attractiveness = attractiveness;
        this.connectionType = connectionType;
        this.deviceId = deviceId;
    }
    public APP getApp() {
        return app;
    }
    public double getDevice_latitude() {
        return mobiledevice_latitude;
    }
    public double getDevice_longitude() {
        return mobiledevice_longitude;
    }

    public int getDevice_attractiveness() {
        return mobiledevice_attractiveness;
    }
    public int getConnectionType() {
        return connectionType;
    }

    public int getDeviceId() { return deviceId; }
}

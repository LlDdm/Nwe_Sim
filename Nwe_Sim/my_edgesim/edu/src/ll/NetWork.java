package ll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.lang.Math.max;

class NetWork {
    private int WAN_BW;
    private int MAN_BW;
    private int GSM_BW;
    private int WLAN_BW;
    private int LAN_BW;
    public Map<EdgeDevice,Map<EdgeDevice,Integer>> BWmap = new HashMap<>();
    public Map<MobileDevice,Integer> mobileBW = new HashMap<>();
    private Random rand = new Random();

    public NetWork() {}

    public int getWAN_BW() {
        return generateNormalint(WAN_BW, WAN_BW * 10 / 100);
    }
    public void setWAN_BW(int WAN_BW) {
        this.WAN_BW = WAN_BW;
    }

    public int getMAN_BW() {
        return generateNormalint(MAN_BW, MAN_BW * 10 / 100);
    }
    public void setMAN_BW(int MAN_BW) {
        this.MAN_BW = MAN_BW;
    }

    public int getLAN_BW() {return generateNormalint(LAN_BW, LAN_BW * 10 / 100);}
    public void setLAN_BW(int LAN_BW) {
        this.LAN_BW = LAN_BW;
    }

    public int getWLAN_BW() {
        return generateNormalint(WLAN_BW, WLAN_BW * 10 / 100);
    }
    public void setWLAN_BW(int wlanBw) {
        this.WLAN_BW = wlanBw;
    }

    public int getGSM_BW() { return generateNormalint(GSM_BW, GSM_BW * 10 / 100); }
    public void setGSM_BW(int GSM_BW) {
        this.GSM_BW = GSM_BW;
    }


    public void initialize(){
        this.MAN_BW = (int) SimSettings.getInstance().getManBandwidth();
        this.WAN_BW = (int) SimSettings.getInstance().getWanBandwidth();
        this.GSM_BW = (int) SimSettings.getInstance().getGsmBandwidth();
        this.WLAN_BW = (int) SimSettings.getInstance().getWlanBandwidth();
        this.LAN_BW = (int) SimSettings.getInstance().getLanBandwidth();
        List<EdgeDevice> edgeDevices = SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdge_devices();

        // 初始化二维 map 并填充对称的带宽数据
        for(EdgeDevice edgeDevice: edgeDevices){
            BWmap.putIfAbsent(edgeDevice, new HashMap<>());
            for(EdgeDevice edgeDevice1: edgeDevices) {
                if (BWmap.get(edgeDevice).get(edgeDevice1) == null) {
                    BWmap.putIfAbsent(edgeDevice1, new HashMap<>());
                    int BW;
                    // 判断设备之间的带宽类型
                    if (edgeDevice.getDeviceId() == 0 || edgeDevice1.getDeviceId() == 0) {
                        BW = getWAN_BW();  // 获取 WAN 带宽
                    } else if (edgeDevice.getAttractiveness() == edgeDevice1.getAttractiveness()) {
                        BW = getLAN_BW();  // 获取 LAN 带宽
                    } else {
                        BW = getMAN_BW();  // 获取 MAN 带宽
                    }

                    // 设置对称的带宽数据
                    BWmap.get(edgeDevice).put(edgeDevice1, BW);
                    BWmap.get(edgeDevice1).put(edgeDevice, BW);  // 保证对称性
                }
            }
        }

    }

    public void generateMobileBW(){
        for(MobileDevice mobileDevice : SimManager.getInstance().getLoadGeneratorModel().getMobileDevices()){
            if(mobileDevice.getConnectionType() == 0){
                mobileBW.put(mobileDevice,getLAN_BW());
            }else if(mobileDevice.getConnectionType() == 1){
                mobileBW.put(mobileDevice,getWLAN_BW());
            }else if(mobileDevice.getConnectionType() == 2){
                mobileBW.put(mobileDevice,getGSM_BW());
            }else
                mobileBW.put(mobileDevice,Integer.MAX_VALUE);
        }
    }

    private int generateNormalint(int mean, int stdDev){
        return (int) Math.round(rand.nextGaussian() * stdDev + mean);
    }
}


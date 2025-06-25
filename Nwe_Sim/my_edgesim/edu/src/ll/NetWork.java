package ll;

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
    }

    private int generateNormalint(int mean, int stdDev){
        return (int) Math.round(rand.nextGaussian() * stdDev + mean);
    }
}


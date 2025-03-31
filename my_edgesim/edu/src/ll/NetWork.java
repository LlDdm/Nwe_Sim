package ll;

import static java.lang.Math.max;

class NetWork {
    private double WAN_BW;
    private double MAN_BW;
    private double[][] attractiveness_BW;
    private int NumofMobileDevices;
    private int NumofAttractiveness;

    public NetWork(int NumofMobileDevices, int NumofAttractiveness) {
        this.attractiveness_BW = new double[NumofMobileDevices][3];
        this.NumofMobileDevices = NumofMobileDevices;
        this.NumofAttractiveness = NumofAttractiveness;
    }

    public double getWAN_BW() {
        return WAN_BW;
    }
    public void setWAN_BW(double WAN_BW) {
        this.WAN_BW = WAN_BW;
    }
    public double getMAN_BW() {
        return MAN_BW;
    }
    public void setMAN_BW(double MAN_BW) {
        this.MAN_BW = MAN_BW;
    }

    public double getLAN_BW(int attractiveness) {
        return attractiveness_BW[attractiveness][0];
    }
    public void setLAN_BW(int attractiveness,double LAN_BW) {
        this.attractiveness_BW[attractiveness][0] = LAN_BW;
    }
    public double getWLAN_BW(int attractiveness) {
        return attractiveness_BW[attractiveness][1];
    }
    public void setWLAN_BW(int attractiveness,double LAN_BW) {
        this.attractiveness_BW[attractiveness][1] = LAN_BW;
    }

    public double getGSM_BW(int attractiveness) {
        return attractiveness_BW[attractiveness][2];
    }

    public void setGSM_BW(int attractiveness,double GSM_BW) {
        this.attractiveness_BW[attractiveness][2] = GSM_BW;
    }

    public int devicecount(int attractiveness) {
        int devicecount=0;
        for(int i=0; i<NumofMobileDevices;i++){
            if(attractiveness == SimManager.getInstance().getLoadGeneratorModel().getMobileDevices().get(i).getDevice_attractiveness()) {
                devicecount++;
            }
        }
        return devicecount;
    }

    public void initialize(){
        this.MAN_BW = SimSettings.getInstance().getManBandwidth();
        this.WAN_BW = SimSettings.getInstance().getWanBandwidth();
        for(int i=0; i<NumofAttractiveness;i++){
            this.attractiveness_BW[i][0] = max(SimSettings.getInstance().getLanBandwidth()-devicecount(i),20);
            this.attractiveness_BW[i][1] = max(SimSettings.getInstance().getWlanBandwidth()-devicecount(i),20);
            this.attractiveness_BW[i][2] = max(SimSettings.getInstance().getGsmBandwidth()-devicecount(i),20);
        }
    }

}


package ll;

public class APP {
    private int Appid;
    private String AppName;
    private double startTime, deadline;
    private long inputsize;
    private long outputsize;
    private long lenth;
    private DAG dag;
    private double CCR;
    private double shape_factor;
    private double completeTime;
    private int mobileDeviceId;

    public APP(int Appid, String AppName, double startTime,double deadline,
               long inputsize, long outputsize, long lenth, DAG dag,double CCR, double shape_factor ,int mobileDeviceId) {
        this.Appid = Appid;
        this.AppName = AppName;
        this.startTime = startTime;
        this.deadline = deadline;
        this.inputsize = inputsize;
        this.outputsize = outputsize;
        this.lenth = lenth;
        this.dag = dag;
        this.CCR = CCR;
        this.shape_factor = shape_factor;
        this.mobileDeviceId = mobileDeviceId;
    }

    public int getAppid() {
        return Appid;
    }

    public String getAppName() {
        return AppName;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getDeadline() {
        return deadline;
    }

    public double getCompleteTime() {
        return completeTime;
    }

    public long getInputsize() {
        return inputsize;
    }

    public long getOutputsize() {
        return outputsize;
    }

    public long getLenth() {
        return lenth;
    }

    public double getShape_factor() {
        return shape_factor;
    }

    public double getCCR() {
        return CCR;
    }

    public DAG getDag() {
        return dag;
    }

    public void setAppName(String appName) {
        AppName = appName;
    }

    public void setAppid(int appid) {
        Appid = appid;
    }

    public void setCCR(double CCR) {
        this.CCR = CCR;
    }

    public void setCompleteTime(double completeTime) {
        this.completeTime = completeTime;
    }

    public void setDag(DAG dag) {
        this.dag = dag;
    }

    public void setDeadline(double deadline) {
        this.deadline = deadline;
    }

    public void setInputsize(long inputsize) {
        this.inputsize = inputsize;
    }

    public void setLenth(long lenth) {
        this.lenth = lenth;
    }

    public void setOutputsize(long outputsize) {
        this.outputsize = outputsize;
    }

    public void setShape_factor(double shape_factor) {
        this.shape_factor = shape_factor;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public int getMobileDeviceId() { return mobileDeviceId; }

}


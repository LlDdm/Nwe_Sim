package ll;

import java.util.*;

public class NativeEdgeDeviceGenerator {
    private final Map<Integer, EdgeDevice> NativeDevicesMap;
    private final Random rand = new Random();
    private Map<Integer, List<EdgeDevice>> edgeDeviceCluster;

    public NativeEdgeDeviceGenerator() {
        this.NativeDevicesMap = new HashMap<>();
        this.edgeDeviceCluster = new HashMap<>();
    }

    public void initialize(){
        List<EdgeDevice> devices = SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdge_devices();
        List<EdgeDevice> edgeDevices = devices.subList(1, devices.size());
        int attractiveness_num = SimSettings.getInstance().Attractiveness_NUM;
        for(int i=0; i<attractiveness_num - 1; i++){
            List<EdgeDevice> Edges = new ArrayList<>();
            for(EdgeDevice edge : edgeDevices){
                if(edge.getAttractiveness() == i)
                    Edges.add(edge);
            }
            edgeDeviceCluster.put(i, Edges);
        }

        for(int i=0; i<attractiveness_num - 1; i++){
            int ran = rand.nextInt(1, edgeDeviceCluster.get(i).size() + 1);
            ran -= 1;
            EdgeDevice edge =  edgeDeviceCluster.get(i).get(ran);
            Scheduler scheduler = new Scheduler(edge);
            edge.setScheduler(scheduler);
            NativeDevicesMap.put(i, edge);
        }
    }

    public Map<Integer, EdgeDevice> getNativeDevicesMap() {
        return NativeDevicesMap;
    }

    public Map<Integer, List<EdgeDevice>> getEdgeDeviceCluster() {return edgeDeviceCluster;}
}

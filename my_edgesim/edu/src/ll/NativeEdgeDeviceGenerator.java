package ll;

import java.util.*;

public class NativeEdgeDeviceGenerator {
    private final Map<Integer, NativeEdgeDevice> NativeDevicesMap;
    private final Random rand = new Random();

    public NativeEdgeDeviceGenerator() { this.NativeDevicesMap = new HashMap<Integer, NativeEdgeDevice>(); }

    public void initialize(){
        List<EdgeDevice> edges = SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdge_devices();
        int attractiveness_num = SimSettings.getInstance().Attractiveness_NUM;
        List<List<EdgeDevice>> Att_Edges = new ArrayList<>();
        for(int i=0; i<attractiveness_num; i++){
            List<EdgeDevice> Edges = new ArrayList<>();
            Att_Edges.add(Edges);
            for(EdgeDevice edge : edges){
                if(edge.getAttractiveness() == i)
                    Att_Edges.get(i).add(edge);
            }
        }

        for(int i=0; i<attractiveness_num; i++){
            int ran = rand.nextInt(Att_Edges.get(i).size());
            NativeDevicesMap.put(i, new Att_Edges.get(i).get(ran));
        }
    }

    public Map<Integer, EdgeDevice> getNativeDevicesMap() {
        return NativeDevicesMap;
    }
}

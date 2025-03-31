package ll;

import java.util.ArrayList;
import java.util.List;

public class EdgeDeviceGeneratorModel {
    private List<EdgeDevice> edge_devices;

    public EdgeDeviceGeneratorModel() {
        edge_devices = new ArrayList<>();
    }

    public void initialize(){
        double [][] Edgedeviceslookuptable = SimSettings.getInstance().getEdgedeviceLookUpTable();
        for(int i=0; i < Edgedeviceslookuptable.length; i++){
            edge_devices.add(new EdgeDevice(SimSettings.getInstance().getEdgedeviceName(i),
                    Edgedeviceslookuptable[i][0], Edgedeviceslookuptable[i][1],Edgedeviceslookuptable[i][2],
                    Edgedeviceslookuptable[i][3], Edgedeviceslookuptable[i][4], Edgedeviceslookuptable[i][5]));
        }
    }

    public List<EdgeDevice> getEdge_devices() {
        return edge_devices;
    }
}

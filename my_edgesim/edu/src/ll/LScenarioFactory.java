/*
 * Title:        EdgeCloudSim - Scenario Factory
 *
 * Description:  Sample scenario factory providing the default
 *               instances of required abstract classes
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package ll;

public class LScenarioFactory implements ScenarioFactory {
    private int numOfMobileDevice;
    private double simulationTime;
    private String orchestratorPolicy;
    private String simScenario;
    private int Attractiveness_NUM;

    LScenarioFactory(int _numOfMobileDevice,
                     double _simulationTime,
                     String _orchestratorPolicy,
                     String _simScenario,
                     int Attractiveness_NUM){
        orchestratorPolicy = _orchestratorPolicy;
        numOfMobileDevice = _numOfMobileDevice;
        simulationTime = _simulationTime;
        simScenario = _simScenario;
    }

    @Override
    public LoadGeneratorModel getLoadGeneratorModel() {
        return new LoadGeneratorModel(numOfMobileDevice, simulationTime, simScenario);
    }

    @Override
    public EdgeDeviceGeneratorModel getDeviceGeneratorModel() {
        return new EdgeDeviceGeneratorModel();
    }

    @Override
    public Scheduler getScheduler() {
        return new Scheduler(orchestratorPolicy, simScenario);
    }

    @Override
    public NetWork getNetworkModel() {
        return new NetWork(numOfMobileDevice, Attractiveness_NUM);
    }

    @Override
    public ResourceMonitor getResourceMonitor() {
        return new ResourceMonitor();
    }

    @Override
    public NativeEdgeDeviceGenerator getNativeEdgeDeviceGenerator() {
        return new NativeEdgeDeviceGenerator();
    }
}



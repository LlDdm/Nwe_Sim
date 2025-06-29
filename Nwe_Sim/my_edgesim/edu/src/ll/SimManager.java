/*
 * Title:        EdgeCloudSim - Simulation Manager
 *
 * Description:
 * SimManager is an singleton class providing many abstract classeses such as
 * Network Model, Mobility Model, Edge Orchestrator to other modules
 * Critical simulation related information would be gathered via this class
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package ll;

import jdk.jshell.spi.SPIResolutionException;

import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class SimManager{
    private String simScenario;
    private String orchestratorPolicy;
    private int numOfMobileDevice;
    private ScenarioFactory scenarioFactory;
    private String useScenario;
    public List<Long> result;
    public CountDownLatch wait_complete;
    private volatile boolean running;
    public int OverDeadline;

    private NetWork networkModel;
    private EdgeDeviceGeneratorModel edgeDeviceGeneratorModel;
    private LoadGeneratorModel loadGeneratorModel;
    private NativeEdgeDeviceGenerator nativeEdgeDeviceGenerator;

    private static SimManager instance = null;

    public SimManager(ScenarioFactory _scenarioFactory) {
        scenarioFactory = _scenarioFactory;
        result = new ArrayList<>();
        instance = this;
        this.OverDeadline = 0;
    }

    public static SimManager getInstance(){
        return instance;
    }

    public void setEdgeDeviceGeneratorModel() {
        System.out.println("Creating Devices...");
        edgeDeviceGeneratorModel = scenarioFactory.getDeviceGeneratorModel();
        edgeDeviceGeneratorModel.initialize();
        System.out.println("Done.");
        instance = this;
    }

    public void setNativeEdgeDeviceGenerator() {
        System.out.println("Creating NativeEdgeDevices...");
        nativeEdgeDeviceGenerator = scenarioFactory.getNativeEdgeDeviceGenerator();
        nativeEdgeDeviceGenerator.initialize();
        System.out.println("Done.");
        instance = this;
    }

    public void setLoadGeneratorModel() {
        System.out.println("Creating Loads...");
        loadGeneratorModel = scenarioFactory.getLoadGeneratorModel();
        loadGeneratorModel.initializeModel();
        System.out.println("Done.");
        instance = this;
    }

    public void setNetworkModel() {
        //Generate network model
        System.out.println("Creating Networks...");
        networkModel = scenarioFactory.getNetworkModel();
        networkModel.initialize();
        System.out.println("Done.");
        instance = this;
    }

    /**
     * Triggering CloudSim to start simulation
     */


    public void startSimulation(){
        //Starts the simulation

        System.out.println("start devices...");
        for(int i=0; i<edgeDeviceGeneratorModel.getEdge_devices().size(); i++){
            edgeDeviceGeneratorModel.getEdge_devices().get(i).startDevice();
        }
        System.out.println("device start done.");

        System.out.println("start scheduler...");
        for(int i=0 ;i < nativeEdgeDeviceGenerator.getNativeDevicesMap().size();i++){
            nativeEdgeDeviceGenerator.getNativeDevicesMap().get(i).scheduler.startDevice(orchestratorPolicy);
        }
        System.out.println("schedule start done.");

        System.out.println("start mobile devices...");
        for(int i=0 ;i < numOfMobileDevice;i++){
            loadGeneratorModel.getMobileDevices().get(i).startDevice();
        }
        System.out.println("mobile device start done.");

    }

    public String getSimulationScenario(){
        return simScenario;
    }

    public String getOrchestratorPolicy(){return orchestratorPolicy;}

    public ScenarioFactory getScenarioFactory(){return scenarioFactory;}

    public int getNumOfMobileDevice(){return numOfMobileDevice;}

    public String getUseScenario(){return useScenario;}

    public void setManager_condition(String orchestratorPolicy, String simScenario, String useScenario, int numOfMobileDevice){
        this.orchestratorPolicy = orchestratorPolicy;
        this.simScenario = simScenario;
        this.useScenario = useScenario;
        this.numOfMobileDevice = numOfMobileDevice;
    }



    public NetWork getNetworkModel(){
        return networkModel;
    }

    public LoadGeneratorModel getLoadGeneratorModel(){
        return loadGeneratorModel;
    }

    public EdgeDeviceGeneratorModel getEdgeDeviceGeneratorModel(){
        return edgeDeviceGeneratorModel;
    }

    public NativeEdgeDeviceGenerator getNativeEdgeDeviceGenerator(){ return nativeEdgeDeviceGenerator; }

    // 停止线程的公共方法
    public void stopRunning() {
        running = false;
    }

    public void Running(){
        running = true;
    }

    public boolean isRunning() { return running; }

}


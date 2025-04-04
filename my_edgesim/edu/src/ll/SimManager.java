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

import java.io.IOException;
import java.util.List;


public class SimManager{
    private String simScenario;
    private String orchestratorPolicy;
    private int numOfMobileDevice;
    private ScenarioFactory scenarioFactory;

    private NetWork networkModel;
    private EdgeDeviceGeneratorModel edgeDeviceGeneratorModel;
    private Scheduler scheduler;
    private LoadGeneratorModel loadGeneratorModel;
    private ResourceMonitor resourceMonitor;

    private static SimManager instance = null;

    public SimManager(ScenarioFactory _scenarioFactory, int _numOfMobileDevice, String _simScenario, String _orchestratorPolicy) throws Exception {
        simScenario = _simScenario;
        scenarioFactory = _scenarioFactory;
        numOfMobileDevice = _numOfMobileDevice;
        orchestratorPolicy = _orchestratorPolicy;

        System.out.println("Creating tasks...");
        loadGeneratorModel = scenarioFactory.getLoadGeneratorModel();
        loadGeneratorModel.initializeModel();
        System.out.println("Done.");

        System.out.println("Creating devices...");
        edgeDeviceGeneratorModel = scenarioFactory.getDeviceGeneratorModel();
        edgeDeviceGeneratorModel.initialize();
        System.out.println("Done.");

        //Generate network model
        networkModel = scenarioFactory.getNetworkModel();
        networkModel.initialize();

        //Generate edge orchestrator
        scheduler = scenarioFactory.getScheduler();
        scheduler.initialize();

        resourceMonitor = scenarioFactory.getResourceMonitor();
        resourceMonitor.initialize();

        instance = this;
    }

    public static SimManager getInstance(){
        return instance;
    }

    /**
     * Triggering CloudSim to start simulation
     */
    public void startSimulation() throws Exception{
        //Starts the simulation
        System.out.println("is starting...");

    }

    public String getSimulationScenario(){
        return simScenario;
    }

    public String getOrchestratorPolicy(){
        return orchestratorPolicy;
    }

    public ScenarioFactory getScenarioFactory(){
        return scenarioFactory;
    }

    public int getNumOfMobileDevice(){
        return numOfMobileDevice;
    }

    public NetWork getNetworkModel(){
        return networkModel;
    }

    public Scheduler getEdgeOrchestrator(){
        return scheduler;
    }

    public LoadGeneratorModel getLoadGeneratorModel(){
        return loadGeneratorModel;
    }

    public ResourceMonitor getResourceMonitor(){ return resourceMonitor; }

    public EdgeDeviceGeneratorModel getEdgeDeviceGeneratorModel(){
        return edgeDeviceGeneratorModel;
    }



}


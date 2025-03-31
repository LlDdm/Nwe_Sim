/*
 * Title:        EdgeCloudSim - Main Application
 *
 * Description:  Main application for Sample App2
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package ll;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Main {

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {
        int iterationNumber = 1;
        String configFile = "";
        String outputFolder = "";
        String edgeDevicesFile = "";
        String applicationsFile = "";
        if (args.length == 5){
            configFile = args[0];
            edgeDevicesFile = args[1];
            applicationsFile = args[2];
            outputFolder = args[3];
            iterationNumber = Integer.parseInt(args[4]);
        }
        else{
            System.out.println("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
            configFile = "property/scenario.properties";
            applicationsFile = "property/applications.xml";
            edgeDevicesFile = "property/devices.xml";
            outputFolder = "sim_results/ite" + iterationNumber;
        }

        //load settings from configuration file
        SimSettings SS = SimSettings.getInstance();
        if(!SS.initialize(configFile, edgeDevicesFile, applicationsFile)){
            System.out.println("cannot initialize simulation settings!");
            System.exit(0);
        }

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date SimulationStartDate = Calendar.getInstance().getTime();
        String now = df.format(SimulationStartDate);
        System.out.println("Simulation started at " + now);
        System.out.println("----------------------------------------------------------------------");

        for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
        {
            for(int k=0; k<SS.getSimulationScenarios().length; k++)
            {
                for(int i=0; i<SS.getOrchestratorPolicies().length; i++)
                {
                    String simScenario = SS.getSimulationScenarios()[k];
                    String orchestratorPolicy = SS.getOrchestratorPolicies()[i];
                    Date ScenarioStartDate = Calendar.getInstance().getTime();
                    now = df.format(ScenarioStartDate);

                    System.out.println("Scenario started at " + now);
                    System.out.println("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - #iteration: " + iterationNumber);;
                    System.out.println("Duration: " + SS.getSimulationTime()/60 + " min (warm up period: "+ SS.getWarmUpPeriod()/60 +" min) - #devices: " + j);
                    System.out.println("RESULT_" + simScenario + "_"  + orchestratorPolicy + "_" + j + "DEVICES");

                    try
                    {
                        Calendar calendar = Calendar.getInstance();

                        // Generate EdgeCloudsim Scenario Factory
                        ScenarioFactory sampleFactory = new LScenarioFactory(j,SS.getSimulationTime(), orchestratorPolicy, simScenario);

                        // Generate EdgeCloudSim Simulation Manager
                        SimManager manager = new SimManager(sampleFactory, j, simScenario, orchestratorPolicy);

                        // Start simulation
                        manager.startSimulation();
                    }
                    catch (Exception e)
                    {
                        SimLogger.printLine("The simulation has been terminated due to an unexpected error");
                        e.printStackTrace();
                        System.exit(0);
                    }

                    Date ScenarioEndDate = Calendar.getInstance().getTime();
                    now = df.format(ScenarioEndDate);
                    SimLogger.printLine("Scenario finished at " + now +  ". It took " + SimUtils.getTimeDifference(ScenarioStartDate,ScenarioEndDate));
                    SimLogger.printLine("----------------------------------------------------------------------");
                }//End of orchestrators loop
            }//End of scenarios loop
        }//End of mobile devices loop

        Date SimulationEndDate = Calendar.getInstance().getTime();
        now = df.format(SimulationEndDate);
        SimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));
    }
}

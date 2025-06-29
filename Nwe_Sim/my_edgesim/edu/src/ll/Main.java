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
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class Main {

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {

        Map<String, Map<Integer, Double>> total_result = new HashMap<>();

        int iterationNumber = 1;
        String configFile = "";
        //String outputFolder = "";
        String edgeDevicesFile = "";
        String applicationsFile = "";
        if (args.length == 5) {
            configFile = args[0];
            edgeDevicesFile = args[1];
            applicationsFile = args[2];
            //outputFolder = args[3];
            iterationNumber = Integer.parseInt(args[4]);
        } else {
            System.out.println("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
            configFile = "my_edgesim/edu/property/scenario.properties";
            applicationsFile = "my_edgesim/edu/property/applications.xml";
            edgeDevicesFile = "my_edgesim/edu/property/devices.XML";
            //outputFolder = "sim_results/ite" + iterationNumber;
        }

        //load settings from configuration file
        SimSettings SS = SimSettings.getInstance();
        if (!SS.initialize(configFile, edgeDevicesFile, applicationsFile)) {
            System.out.println("cannot initialize simulation settings!");
            System.exit(0);
        }

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date SimulationStartDate = Calendar.getInstance().getTime();
        String now = df.format(SimulationStartDate);
        System.out.println("Simulation started at " + now);
        System.out.println("----------------------------------------------------------------------");

        // Generate EdgeCloudsim Scenario Factory
        ScenarioFactory sampleFactory = new LScenarioFactory(SS.getLOAD_GENERATE_TIME(), (long) SS.getWarmUpPeriod());

        // Generate EdgeCloudSim Simulation Manager
        SimManager manager = new SimManager(sampleFactory);

        manager.setEdgeDeviceGeneratorModel();

        manager.setNativeEdgeDeviceGenerator();

        manager.setNetworkModel();


        for (int k = 0; k < SS.getSimulationScenarios().length; k++) {
            for (int l = 0; l < SS.getUseScenarios().length; l++) {
                for (int i = 0; i < SS.getOrchestratorPolicies().length; i++) {

                    String simScenario = SS.getSimulationScenarios()[k];
                    String orchestratorPolicy = SS.getOrchestratorPolicies()[i];
                    String useScenario = SS.getUseScenarios()[l];
                    String condition = simScenario + "_" + orchestratorPolicy + "_" + useScenario;

                    Map<Integer, Double> appNum_averageMakeSpan = new HashMap<>();
                    Map<Integer, Double> appNum_completeRatio = new HashMap<>();
                    Map<Integer, Double> appNum_avgAppNum= new HashMap<>();

                    for (int j = SS.getMinNumOfMobileDev(); j <= SS.getMaxNumOfMobileDev(); j += SS.getMobileDevCounterSize()) {
                        double average_makeSpan = 0;
                        double avg_completeRatio = 0;
                        double avg_AppNum = 0;

                        sampleFactory.setLScenarioFactory(j, orchestratorPolicy, simScenario, useScenario);
                        manager.setManager_condition(orchestratorPolicy, simScenario, useScenario, j);

                        System.out.println("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - Use_Scenario: " + useScenario + " - #iteration: " + iterationNumber);
                        System.out.println("Duration: " + SS.getSimulationTime() / 60 + " min (warm up period: " + SS.getWarmUpPeriod() + " min) - #devices: " + j);
                        System.out.println(simScenario + " | " + orchestratorPolicy + " | " + useScenario + " | " + j + " DEVICES ");

                        for(int h=0; h < 10 ;h++) {
                            double average_makeSpan_of_loop;
                            double completeRatio_of_loop;

                            long makeSpan = 0;

                            manager.result.clear();

                            Date ScenarioStartDate = Calendar.getInstance().getTime();
                            now = df.format(ScenarioStartDate);
                            System.out.println("loop:" + h + " ,Scenario started at " + now);

                            manager.setLoadGeneratorModel();

                            manager.wait_complete = new CountDownLatch(manager.getLoadGeneratorModel().getApp_num());
                            manager.Running();

                            // Start simulation
                            manager.startSimulation();

                            try {
                                manager.wait_complete.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            manager.stopRunning();

                            Date ScenarioEndDate = Calendar.getInstance().getTime();
                            now = df.format(ScenarioEndDate);
                            System.out.println("Scenario finished at " + now + ". It took " + SimUtils.getTimeDifference(ScenarioStartDate, ScenarioEndDate));
                            System.out.println("----------------------------------------------------------------------");

                            for (long result : manager.result) {
                                makeSpan += result;
                            }

                            int resultSize = manager.result.size();
                            completeRatio_of_loop = (double) manager.OverDeadline / resultSize;
                            average_makeSpan_of_loop = (double) makeSpan / resultSize;

                            avg_completeRatio += completeRatio_of_loop;
                            average_makeSpan += average_makeSpan_of_loop;
                            avg_AppNum +=resultSize;

                        }

                        avg_completeRatio /= 10;
                        average_makeSpan /= 10;
                        avg_AppNum /= 10;

                        appNum_completeRatio.put(j,avg_completeRatio);
                        appNum_averageMakeSpan.put(j, average_makeSpan);
                        appNum_avgAppNum.put(j, avg_AppNum);

                        System.out.println("Device_num_average_completeRatio: " + appNum_completeRatio);
                        System.out.println("Device_num_average_makeSpan: " + appNum_averageMakeSpan);
                        System.out.println("Device_num_average_appNum: " + appNum_avgAppNum);
                    }//End of devices loop

                    total_result.put(condition + "Device_num_average_completeRatio", appNum_completeRatio);
                    total_result.put(condition + "Device_num_average_makeSpan", appNum_averageMakeSpan);
                    total_result.put(condition + "Device_num_average_appNum", appNum_avgAppNum);
                    System.out.println("Condition: " + condition + "done!");

                }//End of orchestrators loop
            }//End of use scenarios loop
        }//End of scenarios loop

        int Devices_Num = 50;
        for (int i = 0; i < SS.getOrchestratorPolicies().length; i++) {
            String simScenario = SS.getSimulationScenarios()[0];
            String orchestratorPolicy = SS.getOrchestratorPolicies()[i];
            String useScenario = SS.getUseScenarios()[0];
            String condition = simScenario + "_" + orchestratorPolicy + "_" + useScenario;

            Map<Integer, Double> CCR_averageMakeSpan = new HashMap<>();
            Map<Integer, Double> CCR_completeRatio = new HashMap<>();
            Map<Integer, Double> CCR_avgAppNum= new HashMap<>();

            for (double CCR = 0.1; CCR < 1; CCR+=0.1) {
                SS.setAPP_CCR(CCR);

                double average_makeSpan = 0;
                double avg_completeRatio = 0;
                double avg_AppNum = 0;

                sampleFactory.setLScenarioFactory(Devices_Num, orchestratorPolicy, simScenario, useScenario);
                manager.setManager_condition(orchestratorPolicy, simScenario, useScenario, Devices_Num);

                System.out.println("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - Use_Scenario: " + useScenario + "CCR " + CCR);
                System.out.println("Duration: " + SS.getSimulationTime() / 60 + " min (warm up period: " + SS.getWarmUpPeriod() + " sce) - #devices: " + Devices_Num);

                for (int h = 0; h < 10; h++) {
                    double average_makeSpan_of_loop;
                    double completeRatio_of_loop;

                    long makeSpan = 0;
                    manager.result.clear();

                    Date ScenarioStartDate = Calendar.getInstance().getTime();
                    now = df.format(ScenarioStartDate);
                    System.out.println("loop:" + h + " ,Scenario started at " + now);

                    manager.setLoadGeneratorModel();

                    manager.wait_complete = new CountDownLatch(manager.getLoadGeneratorModel().getApp_num());
                    manager.Running();

                    // Start simulation
                    manager.startSimulation();

                    try {
                        manager.wait_complete.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    manager.stopRunning();

                    Date ScenarioEndDate = Calendar.getInstance().getTime();
                    now = df.format(ScenarioEndDate);
                    System.out.println("Scenario finished at " + now + ". It took " + SimUtils.getTimeDifference(ScenarioStartDate, ScenarioEndDate));
                    System.out.println("----------------------------------------------------------------------");

                    for (long result : manager.result) {
                        makeSpan += result;
                    }

                    int resultSize = manager.result.size();
                    completeRatio_of_loop = (double) manager.OverDeadline / resultSize;
                    average_makeSpan_of_loop = (double) makeSpan / SimManager.getInstance().result.size();

                    avg_completeRatio += completeRatio_of_loop;
                    average_makeSpan += average_makeSpan_of_loop;
                    avg_AppNum +=resultSize;
                }//End of avg loop

                avg_completeRatio /= 10;
                average_makeSpan /= 10;
                avg_AppNum /= 10;

                CCR_completeRatio.put((int) (CCR*10), avg_completeRatio);
                CCR_averageMakeSpan.put((int) (CCR*10), average_makeSpan);//这里因为类型限制，直接将CCR*10转换为整数
                CCR_avgAppNum.put((int) (CCR*10), avg_AppNum);

                System.out.println("CCR_average_completeRatio: " + CCR_completeRatio);
                System.out.println("CCR_average_makeSpan: " + CCR_averageMakeSpan);
                System.out.println("CCR_avgAppNum: " + CCR_avgAppNum);
            }//End of CCR loop
            total_result.put(condition + "CCR_average_completeRatio", CCR_completeRatio);
            total_result.put(condition + "CCR_average_makeSpan", CCR_averageMakeSpan);
            total_result.put(condition + "CCR_avgAppNum", CCR_avgAppNum);
            System.out.println("Condition: " + condition + "done!");
        }//End of orchestrators loop


        // 创建输出excel文件
        Workbook workbook = new XSSFWorkbook();

        // 创建工作表
        for (String sheet_name : total_result.keySet()) {
            createSheet(workbook, sheet_name, total_result.get(sheet_name));
        }

        // 写入文件
        try (FileOutputStream fileOut = new FileOutputStream("result.xlsx")) {
            workbook.write(fileOut);
            System.out.println("结果已保存到：result.xlsx！");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Date SimulationEndDate = Calendar.getInstance().getTime();
        now = df.format(SimulationEndDate);
        System.out.println("Simulation finished at " + now + ". It took " + SimUtils.getTimeDifference(SimulationStartDate, SimulationEndDate));
    }

    private static void createSheet(Workbook workbook, String sheetName, Map<Integer, Double> data) {
        // 创建工作表
        Sheet sheet = workbook.createSheet(sheetName);

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("app_num");
        headerRow.createCell(1).setCellValue("avg_make_span");

        // 填充数据
        int rowNum = 1;
        for (Integer appNum : data.keySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(appNum);
            row.createCell(1).setCellValue(data.get(appNum));
        }
    }

}

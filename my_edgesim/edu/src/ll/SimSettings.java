/*
 * Title:        EdgeCloudSim - Simulation Settings class
 *
 * Description:
 * SimSettings provides system wide simulation settings. It is a
 * singleton class and provides all necessary information to other modules.
 * If you need to use another simulation setting variable in your
 * config file, add related getter method in this class.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package ll;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SimSettings {
    private static SimSettings instance = null;

    public static final double CLIENT_ACTIVITY_START_TIME = 10;

    //enumarations for the VM types
    public static enum VM_TYPES { MOBILE_VM, EDGE_VM, CLOUD_VM }

    //enumarations for the VM types
    public static enum NETWORK_DELAY_TYPES { WLAN_DELAY, MAN_DELAY, WAN_DELAY, GSM_DELAY }

    //predifined IDs for the components.
    public static final int CLOUD_DATACENTER_ID = 1000;
    public static final int MOBILE_DATACENTER_ID = 1001;
    public static final int EDGE_ORCHESTRATOR_ID = 1002;
    public static final int GENERIC_EDGE_DEVICE_ID = 1003;

    //delimiter for output file.
    public static final String DELIMITER = ";";

    public int Attractiveness_NUM;

    private double SIMULATION_TIME; //minutes unit in properties file
    private double WARM_UP_PERIOD; //minutes unit in properties file
    private double INTERVAL_TO_GET_VM_LOAD_LOG; //minutes unit in properties file
    private double INTERVAL_TO_GET_LOCATION_LOG; //minutes unit in properties file
    private double INTERVAL_TO_GET_AP_DELAY_LOG; //minutes unit in properties file
    private boolean FILE_LOG_ENABLED; //boolean to check file logging option
    private boolean DEEP_FILE_LOG_ENABLED; //boolean to check deep file logging option

    private int MIN_NUM_OF_MOBILE_DEVICES;
    private int MAX_NUM_OF_MOBILE_DEVICES;
    private int MOBILE_DEVICE_COUNTER_SIZE;
    private int WLAN_RANGE;

    private int NUM_OF_EDGE_DEVICES;
    private int NUM_OF_PLACE_TYPES;

    private double WAN_PROPAGATION_DELAY; //seconds unit in properties file，广域网传输延迟
    private double GSM_PROPAGATION_DELAY; //seconds unit in properties file,移动网络传输延迟
    private double LAN_INTERNAL_DELAY; //seconds unit in properties file
    private double BANDWITH_WLAN; //Mbps unit in properties file
    private double BANDWITH_MAN; //Mbps unit in properties file
    private double BANDWITH_WAN; //Mbps unit in properties file
    private double BANDWITH_GSM; //Mbps unit in properties file
    private double BANDWITH_LAN;

    private int NUM_OF_HOST_ON_CLOUD_DATACENTER;
    private int NUM_OF_VM_ON_CLOUD_HOST;
    private int CORE_FOR_CLOUD_VM;
    private int MIPS_FOR_CLOUD_VM; //MIPS
    private int RAM_FOR_CLOUD_VM; //MB
    private int STORAGE_FOR_CLOUD_VM; //Byte

    //移动设备参数，假设移动设备不具备处理能力，不用管
    private int CORE_FOR_VM;
    private int MIPS_FOR_VM; //MIPS
    private int RAM_FOR_VM; //MB
    private int STORAGE_FOR_VM; //Byte

    private String[] SIMULATION_SCENARIOS;
    private String[] ORCHESTRATOR_POLICIES;

    private double[][] appLookUpTable = null;
    private String[] appNames = null;

    private double[][] edgedeviceLookUpTable = null;
    private String[] edgedeviceNames = null;

    private double[][] connectionTypeLookUpTable =new double[3][1];

    private SimSettings() {
        Attractiveness_NUM = 0;
    }

    public static SimSettings getInstance() {
        if(instance == null) {
            instance = new SimSettings();
        }
        return instance;
    }


    public boolean initialize(String propertiesFile, String edgeDevicesFile, String applicationsFile){
        boolean result = false;
        InputStream input = null;
        try {
            input = new FileInputStream(propertiesFile);

            // load a properties file
            Properties prop = new Properties();
            prop.load(input);

            SIMULATION_TIME = (double)60 * Double.parseDouble(prop.getProperty("simulation_time")); //seconds
            WARM_UP_PERIOD = (double)60 * Double.parseDouble(prop.getProperty("warm_up_period")); //seconds
            INTERVAL_TO_GET_VM_LOAD_LOG = (double)60 * Double.parseDouble(prop.getProperty("vm_load_check_interval")); //seconds
            INTERVAL_TO_GET_AP_DELAY_LOG = (double)60 * Double.parseDouble(prop.getProperty("ap_delay_check_interval", "0")); //seconds

            MIN_NUM_OF_MOBILE_DEVICES = Integer.parseInt(prop.getProperty("min_number_of_mobile_devices"));
            MAX_NUM_OF_MOBILE_DEVICES = Integer.parseInt(prop.getProperty("max_number_of_mobile_devices"));
            MOBILE_DEVICE_COUNTER_SIZE = Integer.parseInt(prop.getProperty("mobile_device_counter_size"));

            WAN_PROPAGATION_DELAY = Double.parseDouble(prop.getProperty("wan_propagation_delay", "0"));
            GSM_PROPAGATION_DELAY = Double.parseDouble(prop.getProperty("gsm_propagation_delay", "0"));
            LAN_INTERNAL_DELAY = Double.parseDouble(prop.getProperty("lan_internal_delay", "0"));
            BANDWITH_WLAN = 1000 * Double.parseDouble(prop.getProperty("wlan_bandwidth"));
            BANDWITH_MAN = 1000 * Double.parseDouble(prop.getProperty("man_bandwidth", "0"));
            BANDWITH_WAN = 1000 * Double.parseDouble(prop.getProperty("wan_bandwidth", "0"));
            BANDWITH_GSM =  1000 * Double.parseDouble(prop.getProperty("gsm_bandwidth", "0"));
            BANDWITH_LAN = 1000 * Double.parseDouble(prop.getProperty("lan_bandwidth", "0"));

            NUM_OF_HOST_ON_CLOUD_DATACENTER = Integer.parseInt(prop.getProperty("number_of_host_on_cloud_datacenter"));
            NUM_OF_VM_ON_CLOUD_HOST = Integer.parseInt(prop.getProperty("number_of_vm_on_cloud_host"));
            CORE_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("core_for_cloud_vm"));
            MIPS_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("mips_for_cloud_vm"));
            RAM_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("ram_for_cloud_vm"));
            STORAGE_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("storage_for_cloud_vm"));

            ORCHESTRATOR_POLICIES = prop.getProperty("orchestrator_policies").split(",");

            SIMULATION_SCENARIOS = prop.getProperty("simulation_scenarios").split(",");

            connectionTypeLookUpTable[0][0] = Double.parseDouble(prop.getProperty("lanType_percentage", "0"));
            connectionTypeLookUpTable[1][0] = Double.parseDouble(prop.getProperty("wlanType_percentage", "0"));
            connectionTypeLookUpTable[2][0] = Double.parseDouble(prop.getProperty("GSM_percentage", "0"));

        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                    result = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        parseApplicationsXML(applicationsFile);
        parseEdgeDevicesXML(edgeDevicesFile);

        return result;
    }

    public double[][] getconnectiontypeLookUpTable(){
        return connectionTypeLookUpTable;
    }

    /**
     * returns simulation time (in seconds unit) from properties file
     */
    public double getSimulationTime()
    {
        return SIMULATION_TIME;
    }

    /**
     * returns warm up period (in seconds unit) from properties file
     */
    public double getWarmUpPeriod()
    {
        return WARM_UP_PERIOD;
    }

    /**
     * returns VM utilization log collection interval (in seconds unit) from properties file
     */
    public double getVmLoadLogInterval()
    {
        return INTERVAL_TO_GET_VM_LOAD_LOG;
    }

    /**
     * returns VM location log collection interval (in seconds unit) from properties file
     */
    public double getLocationLogInterval()
    {
        return INTERVAL_TO_GET_LOCATION_LOG;
    }

    /**
     * returns VM location log collection interval (in seconds unit) from properties file
     */
    public double getApDelayLogInterval()
    {
        return INTERVAL_TO_GET_AP_DELAY_LOG;
    }

    /**
     * returns deep statistics logging status from properties file
     */
    public boolean getDeepFileLoggingEnabled()
    {
        return FILE_LOG_ENABLED && DEEP_FILE_LOG_ENABLED;
    }

    /**
     * returns deep statistics logging status from properties file
     */
    public boolean getFileLoggingEnabled()
    {
        return FILE_LOG_ENABLED;
    }

    /**
     * returns WAN propagation delay (in second unit) from properties file
     */
    public double getWanPropagationDelay()
    {
        return WAN_PROPAGATION_DELAY;
    }

    /**
     * returns GSM propagation delay (in second unit) from properties file
     */
    public double getGsmPropagationDelay()
    {
        return GSM_PROPAGATION_DELAY;
    }

    /**
     * returns internal LAN propagation delay (in second unit) from properties file
     */
    public double getInternalLanDelay()
    {
        return LAN_INTERNAL_DELAY;
    }

    /**
     * returns WLAN bandwidth (in Mbps unit) from properties file
     */
    public double getWlanBandwidth()
    {
        return BANDWITH_WLAN;
    }

    public double getLanBandwidth(){return BANDWITH_LAN;}

    public double getGSMBandwidth(){return BANDWITH_GSM;}

    /**
     * returns MAN bandwidth (in Mbps unit) from properties file
     */
    public double getManBandwidth()
    {
        return BANDWITH_MAN;
    }

    /**
     * returns WAN bandwidth (in Mbps unit) from properties file
     */
    public double getWanBandwidth()
    {
        return BANDWITH_WAN;
    }

    /**
     * returns GSM bandwidth (in Mbps unit) from properties file
     */
    public double getGsmBandwidth()
    {
        return BANDWITH_GSM;
    }

    /**
     * returns the minimum number of the mobile devices used in the simulation
     */
    public int getMinNumOfMobileDev()
    {
        return MIN_NUM_OF_MOBILE_DEVICES;
    }

    /**
     * returns the maximum number of the mobile devices used in the simulation
     */
    public int getMaxNumOfMobileDev()
    {
        return MAX_NUM_OF_MOBILE_DEVICES;
    }

    /**
     * returns the number of increase on mobile devices
     * while iterating from min to max mobile device
     */
    public int getMobileDevCounterSize()
    {
        return MOBILE_DEVICE_COUNTER_SIZE;
    }

    /**
     * returns edge device range in meter
     */
    public int getWlanRange()
    {
        return WLAN_RANGE;
    }

    /**
     * returns the number of edge datacenters
     */
    public int getNumOfEdgeDatacenters()
    {
        return NUM_OF_EDGE_DEVICES;
    }

    /**
     * returns the number of different place types
     */
    public int getNumOfPlaceTypes()
    {
        return NUM_OF_PLACE_TYPES;
    }

    /**
     * returns the number of cloud datacenters
     */
    public int getNumOfCloudHost()
    {
        return NUM_OF_HOST_ON_CLOUD_DATACENTER;
    }

    /**
     * returns the number of cloud VMs per Host
     */
    public int getNumOfCloudVMsPerHost()
    {
        return NUM_OF_VM_ON_CLOUD_HOST;
    }

    /**
     * returns the total number of cloud VMs
     */
    public int getNumOfCloudVMs()
    {
        return NUM_OF_VM_ON_CLOUD_HOST * NUM_OF_HOST_ON_CLOUD_DATACENTER;
    }

    /**
     * returns the number of cores for cloud VMs
     */
    public int getCoreForCloudVM()
    {
        return CORE_FOR_CLOUD_VM;
    }

    /**
     * returns MIPS of the central cloud VMs
     */
    public int getMipsForCloudVM()
    {
        return MIPS_FOR_CLOUD_VM;
    }

    /**
     * returns RAM of the central cloud VMs
     */
    public int getRamForCloudVM()
    {
        return RAM_FOR_CLOUD_VM;
    }

    /**
     * returns Storage of the central cloud VMs
     */
    public int getStorageForCloudVM()
    {
        return STORAGE_FOR_CLOUD_VM;
    }

    /**
     * returns RAM of the mobile (processing unit) VMs
     */
    public int getRamForMobileVM()
    {
        return RAM_FOR_VM;
    }

    /**
     * returns the number of cores for mobile VMs
     */
    public int getCoreForMobileVM()
    {
        return CORE_FOR_VM;
    }

    /**
     * returns MIPS of the mobile (processing unit) VMs
     */
    public int getMipsForMobileVM()
    {
        return MIPS_FOR_VM;
    }

    /**
     * returns Storage of the mobile (processing unit) VMs
     */
    public int getStorageForMobileVM()
    {
        return STORAGE_FOR_VM;
    }

    /**
     * returns simulation screnarios as string
     */
    public String[] getSimulationScenarios()
    {
        return SIMULATION_SCENARIOS;
    }

    /**
     * returns orchestrator policies as string
     */
    public String[] getOrchestratorPolicies()
    {
        return ORCHESTRATOR_POLICIES;
    }


    public double[][] getAppLookUpTable()
    {
        return appLookUpTable;
    }

    public double[] getAppProperties(String appName) {
        double[] result = null;
        int index = -1;
        for (int i = 0; i< appNames.length; i++) {
            if (appNames[i].equals(appName)) {
                index = i;
                break;
            }
        }

        if(index >= 0 && index < appLookUpTable.length)
            result = appLookUpTable[index];

        return result;
    }

    public String getAppName(int appType)
    {
        return appNames[appType];
    }

    public double[][] getEdgedeviceLookUpTable()
    {
        return edgedeviceLookUpTable;
    }
    public String getEdgedeviceName(int device_id)
    {
        return edgedeviceNames[device_id];
    }

    private void isAttributePresent(Element element, String key) {
        String value = element.getAttribute(key);
        if (value.isEmpty() || value == null){
            throw new IllegalArgumentException("Attribute '" + key + "' is not found in '" + element.getNodeName() +"'");
        }
    }

    private void isElementPresent(Element element, String key) {
        try {
            String value = element.getElementsByTagName(key).item(0).getTextContent();
            if (value.isEmpty() || value == null){
                throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() +"'");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() +"'");
        }
    }

    private Boolean checkElement(Element element, String key) {
        Boolean result = true;
        try {
            String value = element.getElementsByTagName(key).item(0).getTextContent();
            if (value.isEmpty() || value == null){
                result = false;
            }
        } catch (Exception e) {
            result = false;
        }

        return result;
    }

    private void parseApplicationsXML(String filePath)
    {
        Document doc = null;
        try {
            File devicesFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(devicesFile);
            doc.getDocumentElement().normalize();

            String[] mandatoryAttributes = {
                    "usage_percentage", //usage percentage [0-100]
                    "poisson_interarrival", //poisson mean (sec)
                    "active_period", //active period (sec)
                    "idle_period", //idle period (sec)
                    "data_upload", //avg data upload (KB)
                    "data_download", //avg data download (KB)
                    "app_length", //avg task length (MI)
                    "avg_num_task",//app的平均任务数量
                    "shape_factor",//DAG形状因子
                    "app_CCR",//计算传输比
                    "ev_ds"//正态分布均值方差比
            }; //delay_sensitivity [0-1]

            NodeList appList = doc.getElementsByTagName("application");
            appLookUpTable = new double[appList.getLength()]
                    [mandatoryAttributes.length];

            appNames = new String[appList.getLength()];
            for (int i = 0; i < appList.getLength(); i++) {
                Node appNode = appList.item(i);

                Element appElement = (Element) appNode;
                isAttributePresent(appElement, "name");
                String appName = appElement.getAttribute("name");
                appNames[i] = appName;

                for(int m=0; m<mandatoryAttributes.length; m++){
                    isElementPresent(appElement, mandatoryAttributes[m]);
                    appLookUpTable[i][m] = Double.parseDouble(appElement.
                            getElementsByTagName(mandatoryAttributes[m]).item(0).getTextContent());
                }
            }
        } catch (Exception e) {
            System.out.println("Edge Devices XML cannot be parsed! Terminating simulation...");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void parseEdgeDevicesXML(String filePath) {
        Document doc = null;
        try {
            File devicesFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(devicesFile);
            doc.getDocumentElement().normalize();

            String[] mandatoryAttributes = {
                    "mips",
                    "storage",
                    "latitude",
                    "longitude",
                    "wlan_id",
                    "attractiveness",
            };

            NodeList deviceList = doc.getElementsByTagName("device");
            edgedeviceLookUpTable = new double[deviceList.getLength()]
                    [mandatoryAttributes.length];

            Set<Double> attractivenessType = new HashSet<>();

            edgedeviceNames = new String[deviceList.getLength()];
            for (int i = 0; i < deviceList.getLength(); i++) {
                Node deviceNode = deviceList.item(i);

                Element deviceElement = (Element) deviceNode;
                isAttributePresent(deviceElement, "id");
                String deviceName = deviceElement.getAttribute("id");
                edgedeviceNames[i] = deviceName;

                for (int m = 0; m < mandatoryAttributes.length; m++) {
                    isElementPresent(deviceElement, mandatoryAttributes[m]);
                    edgedeviceLookUpTable[i][m] = Double.parseDouble(deviceElement.
                            getElementsByTagName(mandatoryAttributes[m]).item(0).getTextContent());
                }
                if(!attractivenessType.contains(edgedeviceLookUpTable[i][5])){
                    attractivenessType.add(edgedeviceLookUpTable[i][5]);
                    Attractiveness_NUM++;
                }
            }
        } catch (Exception e) {
            System.out.println("Edge Devices XML cannot be parsed! Terminating simulation...");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

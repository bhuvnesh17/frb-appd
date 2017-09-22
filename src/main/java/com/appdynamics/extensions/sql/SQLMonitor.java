package com.appdynamics.extensions.sql;


import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import static com.appdynamics.TaskInputArgs.PASSWORD_ENCRYPTED;

public class SQLMonitor extends AManagedMonitor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SQLMonitor.class);
    private boolean initialized = false;
    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_PREFIX = "Custom Metrics|Tibco ASG|";
    private MonitorConfiguration configuration;
    private long previousTimestamp,currentTimestamp;

    public SQLMonitor() {
        System.out.println(logVersion());
    }

    private void initialize(Map<String, String> taskArgs) {
        if(!initialized){
            //read the config.
            final String configFilePath = taskArgs.get(CONFIG_ARG);
            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
            conf.setConfigYml(configFilePath);
            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE,
                    MonitorConfiguration.ConfItem.METRIC_PREFIX,MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
            initialized = true;
            previousTimestamp = 0;
        }
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }

    private static String getImplementationVersion() {
        return SQLMonitor.class.getPackage().getImplementationTitle();
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        logVersion();
        if(!initialized){
            initialize(taskArgs);
        }
        logger.debug("The raw arguments are {}", taskArgs);
        previousTimestamp = currentTimestamp;
        currentTimestamp = System.currentTimeMillis();
        if(previousTimestamp != 0) {
            configuration.executeTask();
        }

        logger.info("SQLMonitor monitor run completed successfully.");
        return new TaskOutput("SQLMonitor monitor run completed successfully.");
    }

    private class TaskRunnable implements Runnable {
        public void run() {
            Map<String, ?> config = configuration.getConfigYml();
            if (config != null) {
                List<Map> servers = (List<Map>) config.get("dbServers");
                if (servers != null && !servers.isEmpty()) {
                    for (Map server : servers) {
                        try {
                            SQLMonitorTask task = createTask(server);
                            configuration.getExecutorService().execute(task);
                            /*try {
                                Thread.sleep(100000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }*/
                        } catch (IOException e) {
                            logger.error("Cannot construct JMX uri for {}", Util.convertToString(server.get("displayName"),""));
                        }

                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                logger.error("The config.yml is not loaded due to previous errors.The task will not run");
            }
        }
    }

    private SQLMonitorTask createTask(Map server) throws IOException {
        String encryptionPassword = Util.convertToString(server.get("encryptedPassword"),"");
        String encryptionKey = Util.convertToString(server.get("encryptionKey"),"");
        String connUrl = Util.convertToString(server.get("connectionUrl"),"");
        if(!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptionPassword)){
            String password = getPassword(encryptionKey,encryptionPassword);
            connUrl = connUrl.concat(";password="+password);
        }
        JDBCConnectionAdapter jdbcAdapter = JDBCConnectionAdapter.create(connUrl);
        return new SQLMonitorTask.Builder()
                .metricWriter(configuration.getMetricWriter())
                .metricPrefix(configuration.getMetricPrefix())
                .jdbcAdapter(jdbcAdapter)
                .previousTimestamp(previousTimestamp)
                .currentTimestamp(currentTimestamp)
                .server(server).build();

    }

    private String getPassword(String encryptionKey,String encryptedPassword) {
        java.util.Map<String,String> cryptoMap = Maps.newHashMap();
        cryptoMap.put(PASSWORD_ENCRYPTED,encryptedPassword);
        cryptoMap.put(TaskInputArgs.ENCRYPTION_KEY,encryptionKey);
        return CryptoUtil.getPassword(cryptoMap);
    }

    public static void main(String[] args) throws  TaskExecutionException{

        final SQLMonitor monitor = new SQLMonitor();
        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "/Users/bhuvnesh.kumar/repos/appdynamics/extensions/frb-sql-monitoring-extension/src/test/resources/conf/config.yml");

//        monitor.execute(taskArgs, null);


        //monitor.execute(taskArgs, null);


        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 2, 10, TimeUnit.SECONDS);
    }

}
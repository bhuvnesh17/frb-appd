package com.appdynamics.extensions.sql;

import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.YmlUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;



import org.codehaus.jackson.map.ObjectMapper;

public class SQLMonitorTask implements Runnable{

    private static final String METRIC_SEPARATOR = "|";
    private long previousTimestamp;
    private long currentTimestamp;
    private String metricPrefix;
    private MetricWriteHelper metricWriter;
    private JDBCConnectionAdapter jdbcAdapter;
    private Map server;
    private final Yaml yaml = new Yaml();
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SQLMonitorTask.class);



    public void run() {
        MetricPrinter metricPrinter = new MetricPrinter(metricWriter);
        List<Map> queries = (List<Map>) server.get("queries");
        String dbServerDisplayName = (String) server.get("displayName");
        Connection connection = null;
        if (queries != null && !queries.isEmpty()) {
            try{
                connection = jdbcAdapter.open();
                for(Map query : queries){
                    ResultSet resultSet = null;
                    try {
                        String queryDisplayName = (String)query.get("displayName");
                        String statement = (String) query.get("queryStmt");
                        statement = substitute(statement);
                        resultSet = jdbcAdapter.queryDatabase(connection, statement);
                        List<Column> columns = getColumns(query);
                        while (resultSet.next()) {
                            String metricName = "";
                            for(Column c : columns){
                                if(c.getType().equals("metricPathName")){
                                    metricName = getMetricPrefix(dbServerDisplayName,queryDisplayName) + METRIC_SEPARATOR + resultSet.getString(c.getName());
                                }
//                                else if(c.getName().equals("type")){
//                                    metricPrinter.printMetric(metricName,resultSet.getBigDecimal(c.getName()),c.getAggregationType(),c.getTimeRollupType(),c.getClusterRollupType());
//                                }
                                else if(c.getType().equals("metricValue")){
                                    metricPrinter.printMetric(metricName,resultSet.getBigDecimal(c.getName()),c.getAggregationType(),c.getTimeRollupType(),c.getClusterRollupType());
                                }

                            }
                        }
                    }
                    catch(SQLException e){
                        logger.error("Error in query execution",e);
                    }
                    finally {
                        if(resultSet != null){
                            resultSet.close();
                        }
                        if(connection != null){
                            connection.close();
                        }
                    }
                }
            }
            catch(SQLException e){
                logger.error("Unable to open the jdbc connection",e);
            }
        }
    }

    private String getMetricPrefix(String dbServerDisplayName, String queryDisplayName) {
        return metricPrefix + METRIC_SEPARATOR + dbServerDisplayName + METRIC_SEPARATOR + queryDisplayName;
    }


    private List<Column> getColumns(Map query) {
        AssertUtils.assertNotNull(query.get("columns"),"Queries need to have columns configured.");


        Map<String, Map<String, String>> filter = Maps.newLinkedHashMap();
        filter = filterMap(query, "columns");
        final ObjectMapper mapper = new ObjectMapper(); // jackson’s objectmapper
        final Columns columns = mapper.convertValue(filter, Columns.class);

        return columns.getColumns();
    }

    private Map<String, Map<String, String>> filterMap( Map<String, Map<String, String>> mapOfMaps, String filterKey) {
        Map<String, Map<String, String>> filteredOnKeyMap = Maps.newLinkedHashMap();

        if (Strings.isNullOrEmpty(filterKey))
            return filteredOnKeyMap;

        if (mapOfMaps.containsKey(filterKey)) {
            filteredOnKeyMap.put(filterKey,mapOfMaps.get(filterKey));
        }

//        for (String s : mapOfMaps.keySet()) {
//            String temp = s;
//            if(s == "columns") {
//                boolean bop = true;
//            }
//
//            if (s == filterKey) {
//                filteredOnKeyMap.put(s,mapOfMaps.get(s));
//                break;
//            }
//        }

        return filteredOnKeyMap;
    }

    private String substitute(String statement) {
        String stmt = statement;
        stmt = stmt.replace("{{previousTimestamp}}",Long.toString(previousTimestamp));
        stmt = stmt.replace("{{currentTimestamp}}",Long.toString(currentTimestamp));
        return stmt;
    }

    public static class Builder {
        private SQLMonitorTask task = new SQLMonitorTask();

        Builder metricPrefix (String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        Builder metricWriter (MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        Builder server (Map server) {
            task.server = server;
            return this;
        }

        Builder jdbcAdapter (JDBCConnectionAdapter adapter) {
            task.jdbcAdapter = adapter;
            return this;
        }

        Builder previousTimestamp(long timestamp){
            task.previousTimestamp = timestamp;
            return this;
        }

        Builder currentTimestamp(long timestamp){
            task.currentTimestamp = timestamp;
            return this;
        }

        SQLMonitorTask build () {
            return task;
        }
    }
}

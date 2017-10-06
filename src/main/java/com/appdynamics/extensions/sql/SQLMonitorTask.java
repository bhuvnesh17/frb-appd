package com.appdynamics.extensions.sql;

import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.YmlUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
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

    public void run(){
        MetricPrinter metricPrinter = new MetricPrinter(metricWriter);

        List<Map> queries = (List<Map>) server.get("queries");
        Connection connection = null;
        if (queries != null && !queries.isEmpty()) {

            try{
                for(Map query: queries){
                    connection = getConnection(connection);
                    Map<String , BigDecimal> values = executeQuery(connection, query);
                    printData(values, metricPrinter);

                }
            } catch(SQLException e){
                logger.error("Unable to open the jdbc connection",e);
            } catch (ClassNotFoundException e) {
                logger.error("Unable to load the driver ",e);
            }
        }

    }

    private void printData( Map<String , BigDecimal> values, MetricPrinter metricPrinter){

        for (String key : values.keySet()) {
            metricPrinter.reportMetric(key,values.get(key));
        }
    }
//    public void run2() {
//
//        List<Map> queries = (List<Map>) server.get("queries");
//
//        Connection connection = null;
//        if (queries != null && !queries.isEmpty()) {
//
//            try{
//                for(Map query: queries){
//                    connection = getConnection(connection);
//                    executeQuery(connection, query);
//                }
//            } catch(SQLException e){
//                logger.error("Unable to open the jdbc connection",e);
//            } catch (ClassNotFoundException e) {
//                logger.error("Unable to load the driver ",e);
//            }
//        }
//
//        }


        private Map<String , BigDecimal>  executeQuery(Connection connection, Map query) throws SQLException {

            String dbServerDisplayName = (String) server.get("displayName");
            String queryDisplayName = (String)query.get("displayName");

            ResultSet resultSet = getResultSet(connection, query);

            ColumnGenerator columnGenerator = new ColumnGenerator();
            List<Column> columns = columnGenerator.getColumns(query);

//            List<Column> columns = getColumns(query);

            MetricCollector metricCollector = new MetricCollector(metricPrefix,dbServerDisplayName,queryDisplayName);
            Map<String , BigDecimal> values = metricCollector.goThroughResultSet(resultSet,columns);
            return values;
        }

//        private void goThroughResultSet(ResultSet resultSet, List<Column> columns,  String queryDisplayName) throws SQLException {
//
//        MetricPrinter metricPrinter = new MetricPrinter(metricWriter);
//        while (resultSet.next()){
//            String metricName = "";
//            String dbServerDisplayName = (String) server.get("displayName");;
//            boolean metricPathAlreadyAdded = false;
//            for(Column c: columns){
//
//                if(c.getType().equals("metricPathName")){
////                      System.out.println("\n Column name: " +c.getName() + "");
//                    if(metricPathAlreadyAdded == false){
//                        metricName = getMetricPrefix(dbServerDisplayName,queryDisplayName) + METRIC_SEPARATOR + resultSet.getString(c.getName());
////                      System.out.println("MetricPathName: " + metricName);
//                        metricPathAlreadyAdded = true;
//                    }
//                    else{
//                        metricName = metricName + METRIC_SEPARATOR + resultSet.getString(c.getName());
//                    }
//
//                }
//                else if(c.getType().equals("metricValue")){
////                      System.out.println("\n Column name: " +c.getName() + "");
//                    String metricName1 = metricName +  METRIC_SEPARATOR + c.getName() ;
//                    metricPrinter.printMetric(metricName1,resultSet.getBigDecimal(c.getName()),c.getAggregationType(),c.getTimeRollupType(),c.getClusterRollupType());
////                      System.out.println("MetricValue: "+ metricName1 + "  Result : " +resultSet.getBigDecimal(c.getName()).toString());
//                }
//            }
//
//        }
//
//        }

//        private void goOverColumns(ResultSet resultSet, String metricName,String dbServerDisplayName, String queryDisplayName, boolean metricPathAlreadyAdded, Column c,MetricPrinter metricPrinter) throws SQLException {
//            if(c.getType().equals("metricPathName")){
//                System.out.println("\n Column name: " +c.getName() + "");
//                if(metricPathAlreadyAdded == false){
//                    metricName = getMetricPrefix(dbServerDisplayName,queryDisplayName) + METRIC_SEPARATOR + resultSet.getString(c.getName());
//                    System.out.println("MetricPathName: " + metricName);
//                    metricPathAlreadyAdded = true;
//                }
//                else{
//                    metricName = metricName + METRIC_SEPARATOR + resultSet.getString(c.getName());
//                }
//
//            }
//            else if(c.getType().equals("metricValue")){
//                System.out.println("\n Column name: " +c.getName() + "");
//                String metricName1 = metricName +  METRIC_SEPARATOR + c.getName() ;
//                metricPrinter.printMetric(metricName1,resultSet.getBigDecimal(c.getName()),c.getAggregationType(),c.getTimeRollupType(),c.getClusterRollupType());
//                System.out.println("MetricValue: "+ metricName1 + "  Result : " +resultSet.getBigDecimal(c.getName()).toString());
//            }
//
//        }
        private ResultSet getResultSet(Connection connection, Map query) throws SQLException {
            String statement = (String) query.get("queryStmt");
            statement = substitute(statement);
            ResultSet resultSet = jdbcAdapter.queryDatabase(connection, statement);
            return resultSet;
        }

        private Connection getConnection(Connection connection) throws SQLException, ClassNotFoundException {
            connection = jdbcAdapter.open((String)server.get("driver"));
            return connection;
        }


//    public void run1() {
//        MetricPrinter metricPrinter = new MetricPrinter(metricWriter);
//        List<Map> queries = (List<Map>) server.get("queries");
//        String dbServerDisplayName = (String) server.get("displayName");
//        Connection connection = null;
//        if (queries != null && !queries.isEmpty()) {
//            try{
//                for(Map query : queries){
//                    connection = jdbcAdapter.open((String)server.get("driver"));
//
//                    ResultSet resultSet = null;
//                    try {
//                        String queryDisplayName = (String)query.get("displayName");
//                        String statement = (String) query.get("queryStmt");
//                        statement = substitute(statement);
////                        System.out.print(queryDisplayName + " : " + statement + "\n");
//                        resultSet = jdbcAdapter.queryDatabase(connection, statement);
//                        List<Column> columns = getColumns(query);
//                        while (resultSet.next()) {
//                            String metricName = "";
//                            boolean metricPathAlreadyAdded = false;
//                            for(Column c : columns){
//
//                                if(c.getType().equals("metricPathName")){
////                                    System.out.println("\n Column name: " +c.getName() + "");
//                                    if(metricPathAlreadyAdded == false){
//                                    metricName = getMetricPrefix(dbServerDisplayName,queryDisplayName) + METRIC_SEPARATOR + resultSet.getString(c.getName());
////                                    System.out.println("MetricPathName: " + metricName);
//                                        metricPathAlreadyAdded = true;
//                                    }
//                                    else{
//                                         metricName = metricName + METRIC_SEPARATOR + resultSet.getString(c.getName());
//                                    }
//
//                                }
//                                else if(c.getType().equals("metricValue")){
////                                    System.out.println("\n Column name: " +c.getName() + "");
//                                    String metricName1 = metricName +  METRIC_SEPARATOR + c.getName() ;
//                                    metricPrinter.printMetric(metricName1,resultSet.getBigDecimal(c.getName()),c.getAggregationType(),c.getTimeRollupType(),c.getClusterRollupType());
////                                    System.out.println("MetricValue: "+ metricName1 + "  Result : " +resultSet.getBigDecimal(c.getName()).toString());
//                                }
//
//                            }
//                        }
//                    }
//                    catch(SQLException e){
//                        logger.error("Error in query execution",e);
//                    }
//                    finally {
//                        if(resultSet != null){
//                            resultSet.close();
//                        }
//                        if(connection != null){
//                            connection.close();
//                        }
//                    }
//                }
//            }
//            catch(SQLException e){
//                logger.error("Unable to open the jdbc connection",e);
//            } catch (ClassNotFoundException e) {
//                logger.error("Unable to load the driver ",e);
//            }
//        }
//    }

//    private String getMetricPrefix(String dbServerDisplayName, String queryDisplayName) {
//        return metricPrefix + METRIC_SEPARATOR + dbServerDisplayName + METRIC_SEPARATOR + queryDisplayName ;
//    }


//    private List<Column> getColumns(Map query) {
//        AssertUtils.assertNotNull(query.get("columns"),"Queries need to have columns configured.");
//
//        Map<String, Map<String, String>> filter = Maps.newLinkedHashMap();
//        filter = filterMap(query, "columns");
//        final ObjectMapper mapper = new ObjectMapper(); // jackson’s objectmapper
//        final Columns columns = mapper.convertValue(filter, Columns.class);
//        return columns.getColumns();
//    }
//
//    private Map<String, Map<String, String>> filterMap( Map<String, Map<String, String>> mapOfMaps, String filterKey) {
//        Map<String, Map<String, String>> filteredOnKeyMap = Maps.newLinkedHashMap();
//
//        if (Strings.isNullOrEmpty(filterKey))
//            return filteredOnKeyMap;
//
//        if (mapOfMaps.containsKey(filterKey)) {
//            filteredOnKeyMap.put(filterKey,mapOfMaps.get(filterKey));
//        }
//
//        return filteredOnKeyMap;
//    }

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

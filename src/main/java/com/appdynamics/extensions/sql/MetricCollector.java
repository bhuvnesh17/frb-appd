package com.appdynamics.extensions.sql;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bhuvnesh.kumar on 9/29/17.
 */
public class MetricCollector {
    private static final String METRIC_SEPARATOR = "|";
    private String metricPrefix;
    private String dbServerDisplayName;

    private String queryDisplayName;


    public MetricCollector(String metricPrefix, String dbServerDisplayName, String queryDisplayName ){
        this.metricPrefix = metricPrefix;
        this.dbServerDisplayName = dbServerDisplayName;
        this.queryDisplayName = queryDisplayName;
    }

    public Map<String , BigDecimal> goThroughResultSet(ResultSet resultSet, List<Column> columns) throws SQLException {

        Map<String , BigDecimal> values = new HashMap<String, BigDecimal>();
        while (resultSet.next()){
            String metricName = "";
            boolean metricPathAlreadyAdded = false;
            for(Column c: columns){
                if(c.getType().equals("metricPathName")){
//                    System.out.println("\n Column name: " +c.getName() + "");
                    if(metricPathAlreadyAdded == false){
                        metricName = getMetricPrefix(dbServerDisplayName,queryDisplayName) + METRIC_SEPARATOR + resultSet.getString(c.getName());
//                        System.out.println("MetricPathName: " + metricName);
                        metricPathAlreadyAdded = true;
                    }
                    else{
                        metricName = metricName + METRIC_SEPARATOR + resultSet.getString(c.getName());
                    }

                }
                else if(c.getType().equals("metricValue")){
//                    System.out.println("\n Column name: " +c.getName() + "");
                    String metricName1 = metricName +  METRIC_SEPARATOR + c.getName() ;
//                    System.out.println("MetricValue: "+ metricName1 + "  Result : " +resultSet.getBigDecimal(c.getName()).toString());
                    values.put(metricName1, resultSet.getBigDecimal(c.getName()));
                }
            }

        }
        return values;
    }

    private String getMetricPrefix(String dbServerDisplayName, String queryDisplayName) {
        return metricPrefix + METRIC_SEPARATOR + dbServerDisplayName + METRIC_SEPARATOR + queryDisplayName ;
    }

}

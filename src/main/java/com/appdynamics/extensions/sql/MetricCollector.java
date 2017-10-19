package com.appdynamics.extensions.sql;

import javax.xml.soap.Node;
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
            metricName = getMetricPrefix(dbServerDisplayName,queryDisplayName);
            for(Column c: columns){

                if(c.getType().equals("metricPathName")){
//                    System.out.println("\n Column name: " +c.getName() + "");
                    if(metricPathAlreadyAdded == false){
                        metricName = metricName+ METRIC_SEPARATOR + resultSet.getString(c.getName());
//                        System.out.println("MetricPathName: " + metricName);
                        metricPathAlreadyAdded = true;
                    }
                    else{
                        String restOfthePath = resultSet.getString(c.getName());
                        if(restOfthePath.contains(",")){
                            restOfthePath = restOfthePath.replaceAll(",","-");
                        }
                        metricName = metricName + METRIC_SEPARATOR + restOfthePath;
                    }

                }
                else if(c.getType().equals("metricValue")){
//                    System.out.println("\n Column name: " +c.getName() + "");
                    String metricName1 = metricName +  METRIC_SEPARATOR + c.getName() ;
//                    System.out.println("MetricValue: "+ metricName1 + "  Result : " +resultSet.getBigDecimal(c.getName()).toString());
                    String val = resultSet.getString(c.getName());


//                  Checking for the Node_State, if yes then convert to number and add it

                    int nodeStatusValue = getNodeStatusValue(val);
/*
#  NODE STATUS :
# INITIALIZING : 0
#           UP : 1
#         DOWN : 2
#        READY : 3
#       UNSAFE : 4
#     SHUTDOWN : 5
#   RECOVERING : 6
#         NULL : 7
* */
                    if(nodeStatusValue != 7){
                        BigDecimal metric = new BigDecimal(nodeStatusValue);
                        values.put(metricName1, metric);
//                        System.out.println("metricName : " + metricName1 + " :   "+ metric);


                    }
                    else if(val== null){
//                        System.out.println("metricName : " + metricName1 + " :   "+ "value is null");

                    }
                    else {

                        BigDecimal metric = percentSignFound(val, resultSet, c);

                        values.put(metricName1, metric);
//                        System.out.println("metricName : " + metricName1 + " :   "+ metric);

                    }

                }
            }

        }
        return values;
    }

    private String getMetricPrefix(String dbServerDisplayName, String queryDisplayName) {
        return metricPrefix + METRIC_SEPARATOR + dbServerDisplayName + METRIC_SEPARATOR + queryDisplayName ;
    }

    public enum NodeState {
        INITIALIZING, UP, DOWN, READY, UNSAFE, SHUTDOWN, RECOVERING
    }
    public int getNodeStatusValue(String name){
        for (NodeState st : NodeState.values()) {
            if (st.toString().equals(name)){
                return st.ordinal();
            }
        }
        return 6;
    }


    //                  To check if the string contains a percent sign, if so, remove it

    private BigDecimal percentSignFound(String val, ResultSet resultSet,Column c ) throws SQLException {
        String percentInValue = val.substring(val.length()-1, val.length());
        BigDecimal bigDecimalValue;
        if(percentInValue.equals("%")){
            val = val.substring(0,val.length()-1);
             bigDecimalValue = new BigDecimal(val);

        }else {
            bigDecimalValue = resultSet.getBigDecimal(c.getName());
        }
        return bigDecimalValue;

    }



}

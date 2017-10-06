package com.appdynamics.extensions.sql;

import org.junit.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;


/**
 * Created by bhuvnesh.kumar on 10/5/17.
 */
public class MetricCollectorTest {
    private static final String METRIC_SEPARATOR = "|";
    private String metricPrefix ;
    private String dbServerDisplayName ;

    private String queryDisplayName ;


    @Test
    public void testGoThroughResultSetCorrectValues(){
        Map<String , BigDecimal> values = new HashMap<String, BigDecimal>();

        ResultSet resultSet = mock(ResultSet.class);




    }



}

package com.kato.pro.database;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {
    
    private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();
    
    public static void setDataSourceType(DataSourceType dataSourceType) {
        contextHolder.set(dataSourceType);
    }
    
    public static DataSourceType getDataSourceType() {
        return contextHolder.get() == null ? DataSourceType.PRIMARY : contextHolder.get();
    }
    
    public static void clearDataSourceType() {
        contextHolder.remove();
    }
    
    @Override
    protected Object determineCurrentLookupKey() {
        return getDataSourceType();
    }
}
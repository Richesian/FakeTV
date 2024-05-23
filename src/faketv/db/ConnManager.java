package faketv.db;

import java.sql.*;
import java.util.*;

import faketv.util.Logger;







public class ConnManager
{
    private static ConnManager instance;
    private static int clients;
    private Vector<Driver> drivers;
    private Hashtable<String,DBConnectionPool> pools;
	
	/**
	 * Singleton Constructor
	 * @return
	 */
    public static synchronized ConnManager getInstance()
    {
        if(instance == null)
            instance = new ConnManager();
        clients++;
        return instance;
    }

    private ConnManager()
    {
        drivers = new Vector<Driver>();
        pools = new Hashtable<String,DBConnectionPool>();
        init();
    }

    public void freeConnection(String name, Connection con)
    {
        DBConnectionPool pool = (DBConnectionPool)pools.get(name);
        if(pool != null)
            pool.freeConnection(con);
    }

    public Connection getConnection(String name)
    {
        DBConnectionPool pool = (DBConnectionPool)pools.get(name);
        if(pool != null)
            return pool.getConnection();
        else
            return null;
    }


    private void destroy()
    {
        if(--clients != 0)
            return;
        DBConnectionPool pool;
        for(Enumeration<DBConnectionPool> allPools = pools.elements(); allPools.hasMoreElements(); pool.release())
            pool = (DBConnectionPool)allPools.nextElement();

        for(Enumeration<Driver> allDrivers = drivers.elements(); allDrivers.hasMoreElements();)
        {
            Driver driver = (Driver)allDrivers.nextElement();
            try
            {
                DriverManager.deregisterDriver(driver);
                Logger.log("connection_log","Deregistered JDBC driver " + driver.getClass().getName());
            }
            catch(Exception e)
            {
            	Logger.log("connection_log","Can't deregister JDBC driver: " + driver.getClass().getName());
            }
        }

    }

    private void createPools(Properties props)
    {
        for(Enumeration<?> propNames = props.propertyNames(); propNames.hasMoreElements();)
        {
            String name = (String)propNames.nextElement();
            if(name.endsWith(".url"))
            {
                String poolName = name.substring(0, name.lastIndexOf("."));
                String url = props.getProperty(poolName + ".url");
                if(url == null)
                {
                	Logger.log("connection_log","No URL specified for " + poolName);
                } else
                {
                    String user = props.getProperty(poolName + ".user");
                    String password = props.getProperty(poolName + ".password");
                    String maxconn = props.getProperty(poolName + ".maxconn", "0");
                    String test_sql = props.getProperty(poolName + ".test_sql", "");
                    String timeout_seconds = props.getProperty(poolName + ".timeout","0");
                    int max;
                    try
                    {
                        max = Integer.valueOf(maxconn).intValue();
                    }
                    catch(NumberFormatException e)
                    {
                    	Logger.log("connection_log","Invalid maxconn value " + maxconn + " for " + poolName);
                        max = 0;
                    }
                    DBConnectionPool pool = new DBConnectionPool(poolName, url, user, password, max, test_sql, timeout_seconds);
                    pools.put(poolName, pool);
                    Logger.log("connection_log","Initialized pool " + poolName);
                }
            }
        }

    }

    private void init()
    {
        java.io.InputStream is = getClass().getResourceAsStream("/db.properties");
        Properties dbProps = new Properties();
        try
        {
            dbProps.load(is);
        }
        catch(Exception e)
        {
            System.err.println("Can't read the properties file. Make sure db.properties is in the CLASSPATH");
            return;
        }
        loadDrivers(dbProps);
        createPools(dbProps);
    }

    private void loadDrivers(Properties props)
    {
        String driverClasses = props.getProperty("drivers");
        for(StringTokenizer st = new StringTokenizer(driverClasses); st.hasMoreElements();)
        {
            String driverClassName = st.nextToken().trim();
            try
            {
                Driver driver = (Driver)Class.forName(driverClassName).newInstance();
                DriverManager.registerDriver(driver);
                drivers.addElement(driver);
                Logger.log("connection_log","Registered JDBC driver " + driverClassName);
            }
            catch(Exception e)
            {
            	Logger.log("connection_log","Can't register JDBC driver: " + driverClassName + ", Exception: " + e);
            }
        }

    }
}

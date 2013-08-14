package ru.erhaben.qiwi.database;

import java.sql.DriverManager;
import java.sql.ResultSet;
import ru.erhaben.qiwi.misc.Log;
import ru.erhaben.qiwi.misc.Registry;

public class Provider
{
    private static volatile Provider instance;
    
    public static Provider getInstance()
    {
        if (instance == null)
        {
            synchronized(Provider.class)
            {
                if (instance == null)
                {
                    instance = new Provider();
                }
            }
        }        
        return instance;
    }
    
    // Получить имя провайдера платежа. Если ничего не найжет - вернет пустую строку.
    public String getTitle(String id)
    {
        //Registry.getInstance().window.appendTextToLogArea("Получение названия цели платежа");
        String title = "";
        
        try
        {  
            java.sql.Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Registry.getInstance().PROVIDERS_DATABASE_PATH);
            //java.sql.Connection connection = DriverManager.getConnection("jdbc:sqlite:" + System.getenv("APPDATA") + "/osmp/qiwicashier/providers.db");
            java.sql.Statement statement = connection.createStatement();
            
            String sql = "SELECT `providerID`, `longName` FROM `providers` WHERE `providerID` = '" + id + "' LIMIT 1";
            
            ResultSet result = statement.executeQuery(sql);
            
            if (result.next())
            {
                title = result.getString("longName");
            }
            
            statement.close();
            connection.close();
        }
        catch(Exception ex)
        {
            Log.getInstance().write("provider_database", "error while getting title", ex.toString() + " ( " + ex.getMessage() + " )");                    
            //System.out.println("[Error] Error getting provider's title "+ ex.toString() + " ( " + ex.getMessage() + " )");
            if ( Registry.getInstance().debug)                
                ex.printStackTrace();
        }
        
        return title;
    }
}

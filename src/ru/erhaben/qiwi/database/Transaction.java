package ru.erhaben.qiwi.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import org.json.JSONObject;
import org.json.JSONArray;
import ru.erhaben.qiwi.misc.Log;
import ru.erhaben.qiwi.misc.Registry;
import ru.erhaben.qiwi.network.Socket;

public class Transaction
{
    private static volatile Transaction instance;
    
    /* Локальные переменные для БД */
    private Connection connection;
    private Statement statement;
    
    public static Transaction getInstance()
    {
        if (instance == null)
        {
            synchronized(Transaction.class)
            {
                if (instance == null)
                    instance = new Transaction();
            }
        }        
        return instance;
    }
    
    // Получить файл БД.
    public File getDatabaseFile()
    {
        return new File(Registry.getInstance().PAYMENTS_DATABASE_PATH);
    }
    
    // Создать соединение с БД
    private void openConnection() throws Exception
    {
        //this.connection = DriverManager.getConnection("jdbc:sqlite:" + System.getenv("APPDATA") + "/osmp/qiwicashier/payments.db");
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + Registry.getInstance().PAYMENTS_DATABASE_PATH);
        this.statement = this.connection.createStatement();
    }
    
    // Закрыть соединение с БД
    private void closeConnection()
    {
        try
        {
            this.statement.close();
        }
        catch(Exception ex){  
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            } 
        }
        
        try
        {
            this.connection.close();
        }
        catch(Exception ex){            
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            } 
        }
        
        this.statement = null;
        this.connection = null;       
    }
    
    // Получить ID терминала
    public long getTerminalID()
    {
        Registry.getInstance().window.appendTextToLogArea("Получение номера терминала");
        
        long terminal_id = 0;
        
        try
        {
            this.openConnection();
            
            ResultSet result = this.statement.executeQuery("SELECT terminalID FROM payments LIMIT 1");
            result.next();
            
            terminal_id = result.getLong("terminalID");
            
            if ( terminal_id > 0)
                Registry.getInstance().terminal_id = terminal_id; 
            
            result.close();
        }
        catch(Exception ex)
        {
            Log.getInstance().write("transactions_database", "error while getting terminal id", ex.toString() + " ( " + ex.getMessage() + " )");                    
            //System.out.println("[Error] Cant get terminal from DB "+ ex.toString() + " ( " + ex.getMessage() + " )");
            
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            } 
        }
        this.closeConnection();
        
        return terminal_id;//9410216;//terminal_id;
    }
    
    // Получить все транзакции начиная с какого-то номера. Второй параметр определяет будет ли полученная транзакция сразу же отсылаться на сервер.
    public JSONObject getTransactionsFromID(long id, boolean need_send)
    {
        Registry.getInstance().window.appendTextToLogArea("Получение платежей начиная с №" + Long.toString(id));
        JSONArray rows = new JSONArray();
        JSONObject answer = new JSONObject();
        
        try
        {
            //System.out.println("[Qiwi] Getting transactions from DB ( from " + Long.toString(id) + " )");
            this.openConnection();
                        
            ResultSet result = this.statement.executeQuery("SELECT * FROM payments WHERE `transactionNumber` > " + Long.toString(id));
            ResultSetMetaData metadata = result.getMetaData();
            int columns = metadata.getColumnCount();
            int total_row_count = 0;
            while (result.next() != false)
            {
                JSONObject row = new JSONObject();
                
                for (int i = 1; i <= columns; i++)
                {
                    String column = metadata.getColumnName(i);
                    String value = result.getString(i);

                    row.put(column, value);                       
                }
                
                String providerTitle = Provider.getInstance().getTitle(result.getString("providerID"));
                row.put("providerTitle", providerTitle);
                rows.put(row);

                if (Registry.getInstance().socket_connected)
                {
                    if (need_send)
                    {
                        Socket.emit("register", new JSONObject().put("terminal", Registry.getInstance().terminal_id)
                            .put("id", row.get("transactionNumber"))
                            .put("item", row));
                    
                        Registry.getInstance().last_id = Long.parseLong(row.get("transactionNumber").toString());
                    }                    
                }
                Local.getInstance().saveTransaction(row);
                total_row_count += 1;
            }
            
            //System.out.println("[Qiwi] " + Integer.toString(total_row_count) + " founded");
            
            answer.put("terminal", Registry.getInstance().terminal_id);
            answer.put("rows", rows);
            
            result.close();
        }
        catch(Exception ex)
        {
            Log.getInstance().write("transactions_database", "error while getting transactions from id", ex.toString() + " ( " + ex.getMessage() + " )");                    
            //System.out.println("[Error] Cant get basic transactions from DB "+ ex.toString() + " ( " + ex.getMessage() + " )");
            
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            }                            
        }
        this.closeConnection();
                
        return answer;
    }
    
    // Получить транзакцию ( одну )
    public JSONObject getTransaction(String id)
    {
        //Registry.getInstance().window.appendTextToLogArea("Получение платежа №" + id);
        JSONObject transaction = null;
        
        try
        {
            this.openConnection();
            
            ResultSet result = this.statement.executeQuery("SELECT * FROM payments WHERE `transactionNumber` = " + id + " LIMIT 1");
            ResultSetMetaData metadata = result.getMetaData();
            int columns = metadata.getColumnCount();
            
            while (result.next() != false)
            {
                transaction = new JSONObject();
                
                for (int i = 1; i <= columns; i++)
                {
                   String column = metadata.getColumnName(i);
                   String value = result.getString(i);

                   transaction.put(column, value);                       
                }
                //Local.getInstance().saveTransaction(transaction);
                
                String providerTitle = Provider.getInstance().getTitle(result.getString("providerID"));
                
                transaction.put("providerTitle", providerTitle);
                
            }
            
            result.close();
        }
        catch(Exception ex)
        {
            //System.out.println("[Error] Cant get single transaction from DB "+ ex.toString() + " ( " + ex.getMessage() + " )"); 
            Log.getInstance().write("transactions_database", "error while getting single transaction", ex.toString() + " ( " + ex.getMessage() + " )");                    
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            } 
        }
        this.closeConnection();
        
        return transaction;
    }
}

package ru.erhaben.qiwi.database;

import java.io.File;
import org.json.JSONObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import ru.erhaben.qiwi.misc.Log;
import ru.erhaben.qiwi.misc.Registry;

/**
 * CREATE  TABLE "main"."transactions" ("id_terminal" INTEGER NOT NULL , "id_transaction" INTEGER PRIMARY KEY  NOT NULL  UNIQUE , "executed" INTEGER NOT NULL  DEFAULT 0, "created" INTEGER NOT NULL , "updated" INTEGER)
 * @author erhaben
 */

public class Local
{    
    private static volatile Local instance;
    
    /* Локальные переменные для БД */
    private Connection connection;
    private Statement statement;
    
    private Local(){
        // Азаза
    }
    
    public static Local getInstance()
    {
        if (instance == null)
        {
            synchronized(Transaction.class)
            {
                if (instance == null)
                {
                    instance = new Local();
                }
            }
        }        
        return instance;
    }
    
    // Метод создания файла БД
    private boolean createDatabaseFile(File f)
    {
        boolean result = true;
        Registry.getInstance().window.appendTextToLogArea("Создание локальной базы данных");
        
        try
        {
            String sql = "CREATE  TABLE transactions (\"id_terminal\" INTEGER NOT NULL , \"id_transaction\" INTEGER PRIMARY KEY  NOT NULL  UNIQUE , \"executed\" INTEGER NOT NULL  DEFAULT 0, \"created\" INTEGER NOT NULL , \"updated\" INTEGER)";
            this.statement.executeUpdate(sql);
        }
        catch(Exception ex)
        {
            Log.getInstance().write("local_database", "error while creating db file", ex.toString());                    
            result = false;
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            } 
        }
        
        return result;
    }
    
    // Открытие соединения с БД
    private void openConnection() throws Exception
    {
        boolean is_new_file = false;
        File db_file = new File(Registry.getInstance().LOCAL_DATABASE_PATH);
        if ( ! db_file.exists()){
            is_new_file = true;
        }
        
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + db_file.getAbsolutePath());
        this.statement = this.connection.createStatement();
        
        if ( is_new_file )
        {
            if ( ! this.createDatabaseFile(db_file))
                throw new Exception("Cant create DB file");
        }             
    }
    
    // Закрытие соединения с БД
    private void closeConnection()
    {
        try
        {
            this.statement.close();
        }
        catch(Exception ex){            
        }
        
        try
        {
            this.connection.close();
        }
        catch(Exception ex){            
        }
        
        this.statement = null;
        this.connection = null;
      
    }
    
    // Сохранить транзакцию в БД
    public boolean saveTransaction(JSONObject item)
    {
        boolean result = true;
        
        try
        {
            this.openConnection();
            
            String sql = "INSERT OR IGNORE INTO transactions "
                    + "(id_terminal, id_transaction, executed, created) "
                    + "VALUES "
                    + "(" + item.get("terminalID").toString() + ", " + item.get("transactionNumber").toString() + ", 0, " + item.get("dateTime").toString() + ")";
            
            result = this.statement.execute(sql);              
        }
        catch(Exception ex)
        {
            result = false;
            Log.getInstance().write("local_database", "error while saving transaction", ex.toString() + " ( " + ex.getMessage() + " )");                    
            //System.out.println("[Error] Cant save transaction to local DB "+ ex.toString() + " ( " + ex.getMessage() + " )");  
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            } 
        }
        this.closeConnection();
        
        return result;
    }  
    
    // Проведена ли транзакция
    public boolean isExecuted(String id)
    {
        boolean result = true;
        
        try
        {
            this.openConnection();
            
            String sql = "SELECT `executed`, `id_transaction` FROM `transactions` WHERE `id_transaction` = " + id + " LIMIT 1";
            
            ResultSet rs = statement.executeQuery(sql);
            
            if (rs.next())
            {
                if (rs.getInt("executed") == 0)
                    result = false;
                else
                    result = true;
            }  
            rs.close();
        }
        catch(Exception ex)
        {
            result = false;
            Log.getInstance().write("local_database", "error while checking transaction status", ex.toString() + " ( " + ex.getMessage() + " )");                    
            //System.out.println("[Error] Cant check execution status of transaction in local DB "+ ex.toString() + " ( " + ex.getMessage() + " )");  
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            } 
        }
        this.closeConnection();
        
        return result;
    }
    
    // Получает все транзакции которые не отмечены как выполненные
    public ArrayList<JSONObject> getUnsended()
    {
        ArrayList<JSONObject> transactions = new ArrayList<>();
        
        try
        {
            this.openConnection();
            String sql = "SELECT * FROM `transactions` WHERE `executed` = 0";
            
            ResultSet rs = statement.executeQuery(sql);
            
            while ( rs.next() != false )
            {
                transactions.add(Transaction.getInstance().getTransaction(Integer.toString(rs.getInt("id_transaction"))));            
            }
                
            rs.close();
        }
        catch(Exception ex)
        {
            Log.getInstance().write("local_database", "error while getting unsended transactions", ex.toString() + " ( " + ex.getMessage() + " )");                    
            //System.out.println("[Error] Cant get list of unsended transactions from local DB "+ ex.toString() + " ( " + ex.getMessage() + " )");  
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            } 
        }
        this.closeConnection();
        
        return transactions;
    }
    
    // Пометить транзакцию как выполненную
    public boolean markTransactionAsExecuted(String id)
    {
        boolean result = true;
        
        try
        {
            this.openConnection();
            
            String sql = "UPDATE `transactions` SET `executed` = 1 WHERE `id_transaction` = " + id;
            
            result = this.statement.execute(sql);           
        }
        catch(Exception ex)
        {
            result = false;
            Log.getInstance().write("local_database", "error while marking transaction", ex.toString() + " ( " + ex.getMessage() + " )");                    
            //System.out.println("[Error] Cant mark transaction as executed in local DB "+ ex.toString() + " ( " + ex.getMessage() + " )");  
            
            if ( Registry.getInstance().debug)  
            {
                System.out.println(this.statement);
                System.out.println(this.connection);
                ex.printStackTrace();
            } 
        }
        this.closeConnection();
        
        return result;
    }
}

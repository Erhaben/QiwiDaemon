package ru.erhaben.qiwi.misc;

import java.io.File;
import java.io.PrintWriter;

public class Log
{
    /* Константы */
    static final String LOG_FILE = "qiwi.log";
    
    /* Экземпляр объекта */ 
    private static volatile Log instance; 
    
    private Log(){
        File log_file;
        
        try{
            
            log_file = new File(System.getProperty("user.dir") + "/" + Log.LOG_FILE);
            
            if ( ! log_file.exists()){
                log_file.createNewFile();
           } 
            
        } catch (Exception ex){
            ex.printStackTrace();
        }
        
        log_file = null;
    }
    
    public static Log getInstance()
    {
        if (instance == null)
        {
            synchronized(Registry.class)
            {
                if (instance == null)
                {
                    instance = new Log();
                }
            }
        }
        
        return instance;
    }
    
    // Запись данных в файл лога
    public void write(String module, String message, String data){
        String date = new java.util.Date().toString();
        
        String line = "[ " + date + " ] " + module + ", " + message + " ---->> " + data;
        
        try{
            PrintWriter out = new PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(System.getProperty("user.dir") + "/" + Log.LOG_FILE, true)));
            out.println(line);
            out.close();
        }
        catch(Exception ex){
            // Некрасиво, но именно тут необходимо.
            ex.printStackTrace();
            System.err.println(line);
        }
    }
}

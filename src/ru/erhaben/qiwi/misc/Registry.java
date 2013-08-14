package ru.erhaben.qiwi.misc;

public class Registry
{
    /* Ссылка на основное окно */
    public volatile ru.erhaben.qiwi.gui.MainWindow window;
    
    /* Прочее */
    public final boolean debug = false;
    public final String SOCKET_URL = "ws://dev.tst.drg.ru:8088";//"ws://192.168.63.64:8088";
    
    /* Пути к БД */
    public final String LOCAL_DATABASE_PATH = System.getenv("APPDATA") + "/osmp/qiwicashier/local.sqlite";
    public final String PAYMENTS_DATABASE_PATH = System.getenv("APPDATA") + "/osmp/qiwicashier/payments.db";
    public final String PROVIDERS_DATABASE_PATH = System.getenv("APPDATA") + "/osmp/qiwicashier/providers.db";
    
    /* Экземпляр объекта */ 
    private static volatile Registry instance;
    
    /* Переменные киви */
    public volatile long last_id = -1;
    public volatile long terminal_id = -1;
    
    /* Служебное */
    public final int timeout = 1000 * 3; // 5 секунд
    public volatile boolean socket_connected = false;
    
    private Registry(){
    
    }
    
    public static Registry getInstance()
    {
        if (instance == null)
        {
            synchronized(Registry.class)
            {
                if (instance == null)
                {
                    instance = new Registry();
                }
            }
        }
        
        return instance;
    }
}

package ru.erhaben.qiwi.daemon;

import java.io.File;
import java.util.ArrayList;
import ru.erhaben.qiwi.misc.Registry;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.erhaben.qiwi.database.Local;
import ru.erhaben.qiwi.network.Socket;
import ru.erhaben.qiwi.database.Transaction;
import ru.erhaben.qiwi.misc.Log;

public class DaemonController extends Thread
{
    
    // Конструктор, в котором делается запуск контроллера в отдельном потоке
    public DaemonController()
    {
        this.start();
    }
    
    @Override
    public void run()
    {        
        // Показать ID терминала
        Registry.getInstance().window.showTerminalID(Long.toString(Transaction.getInstance().getTerminalID()));
        
        // Запуск таймера для отправки транзакций, которые не были подтверждены сервером
        this.startUnsendedTransactionsTask();
        
        // Запуск таймера, опрашивающего соединение
        this.startConnectionTask();
        
        // Проверка локальной БД
        this.checkLocalDatabase();
        
        // Цикл опроса БД
        this.loop();
    }
    
    // Основной цикл контроллера
    private void loop()
    {
        Registry.getInstance().window.appendTextToLogArea("Начало работы");
        Log.getInstance().write("daemon_controller", "Begin", "Begin");
        
        // Получаю время последнего изменения файла
        File transactions_db_file = Transaction.getInstance().getDatabaseFile();
        long db_updated_time = transactions_db_file.lastModified();
        
        while(true)
        {
            try
            {
                // Работаем только когда сокет подключен
                if (Registry.getInstance().socket_connected)
                {
                    // Если текущее время изменения файла не навно предидущему
                    if ( transactions_db_file.lastModified() != db_updated_time)
                    {
                        // Обновляю предидущее время
                        db_updated_time = transactions_db_file.lastModified();
                        
                        // Если с номером терминала и последним ID все нормально
                        if (Registry.getInstance().terminal_id > 0 && Registry.getInstance().last_id >= 0){
                            // И сокет все еще подключен
                            if (Registry.getInstance().socket_connected){
                                // Получаем транзакции начиная с последнего ID и сразу же отправляем их на сервер
                                Transaction.getInstance().getTransactionsFromID(Registry.getInstance().last_id, true);
                            }                                    
                        }                            
                    }
                }
               
                // Пауза. В данный момент равна 3-м секундам
                Thread.sleep(Registry.getInstance().timeout);
            }
            catch(Exception ex)
            {       
                Log.getInstance().write("daemon_controller", "exception while loop", ex.toString());
                if ( Registry.getInstance().debug)                
                    ex.printStackTrace();
            }            
        }
    }
    
    // Запуск проверки подключения
    private void startConnectionTask()
    {
        Timer timer = new Timer();
        timer.schedule(new ConnectionTask(), 0, 1000 * 30);
    }
    
    // Запуск отправки неактивных транзакций
    private void startUnsendedTransactionsTask()
    {
        Timer timer = new Timer();
        timer.schedule(new UnsendedTask(), 1000 * 1, 1000 * 60);
    }
    
    // Проверка локальной БД
    private void checkLocalDatabase()
    {
        Registry.getInstance().window.appendTextToLogArea("Проверка локальной базы данных");
        JSONObject o = Transaction.getInstance().getTransactionsFromID(0 ,false);
        
        if ( o.length() > 0)
        {
            JSONArray rows = o.getJSONArray("rows");            
            
            for (int i = 0; i < rows.length(); i++)
            {
                JSONObject row = (JSONObject)rows.get(i);
                Local.getInstance().saveTransaction(row);        
            }
        }
    }
    
    class ConnectionTask extends TimerTask
    {
        @Override
        public void run()
        {
            Socket.init();
        }
    }
    
    class UnsendedTask extends TimerTask
    {
        @Override
        public void run()
        {
            Registry.getInstance().window.appendTextToLogArea("Отправка неактивных транзакций из локальной базы данных");
            if (Registry.getInstance().socket_connected)
            {
                ArrayList<JSONObject> unsended = Local.getInstance().getUnsended();
                
                Registry.getInstance().window.appendTextToLogArea("Найдено " + Integer.toString(unsended.size()));
                
                for (int i = 0; i < unsended.size(); i++)
                {
                    JSONObject transaction = unsended.get(i);
                    
                    try
                    {
                        Socket.emit("register", new JSONObject().put("terminal", Registry.getInstance().terminal_id)
                            .put("id", transaction.get("transactionNumber"))
                            .put("item", transaction));
                        //System.exit(0);
                    }
                    catch(Exception ex)
                    {
                        Log.getInstance().write("daemon_controller", "exception while sending unsended transactions", ex.toString());
                        if ( Registry.getInstance().debug)                
                            ex.printStackTrace();
                    }                   
                }
            } else {
                Registry.getInstance().window.appendTextToLogArea("Нет соединения.");
            }
        }
    }
}

package ru.erhaben.qiwi.network;

import ru.erhaben.qiwi.misc.Registry;
import com.clwillingham.socket.io.MessageCallback;
import org.json.JSONObject;
import ru.erhaben.qiwi.database.Local;
import ru.erhaben.qiwi.database.Transaction;
import ru.erhaben.qiwi.gui.MainWindow;
import ru.erhaben.qiwi.misc.Log;

public class SocketCallback implements MessageCallback
{
    
    @Override
    public void onConnectFailure()
    {
        Registry.getInstance().window.appendTextToLogArea("Соединение прервано");
        //System.out.println("[Qiwi] Connection failed");
        Registry.getInstance().socket_connected = false;
    }

    @Override
    public void on(String event, JSONObject... data) {
        //System.out.println("[Qiwi] Event fired ( " + event + " )");  
        this.processEvent(event, data);
    }

    @Override
    public void onMessage(String message) {
        //System.out.println("[Qiwi] Recieved message from server ( as sring )");
        System.out.println(message);
    }

    @Override
    public void onMessage(JSONObject message) {
        //System.out.println("[Qiwi] Recieved message from server ( as JSON )");
        System.out.println(message.toString());
    }

    @Override
    public void onConnect() {
      //System.out.println("[Qiwi] Socket connected");
      this.showConnectionOn();
      this.displayMessage("Подключен к серверу.");
      Registry.getInstance().socket_connected = true;
          
      try
      {          
          // Получаю id терминала
          if ( Registry.getInstance().terminal_id  == -1)
          {
              long terminal_id = Transaction.getInstance().getTerminalID();
              Registry.getInstance().terminal_id = terminal_id;
              
              if ( terminal_id < 0)
              {
                  Log.getInstance().write("socket_callback", "exception while sending terminal id to server", Long.toString(terminal_id));
              }
          }
          Socket.emit("auth", new JSONObject().put("terminal", Registry.getInstance().terminal_id));                  
      }
      catch(Exception ex)
      {
          Log.getInstance().write("socket_callback", "exception while sending terminal id to server", ex.toString());
          if ( Registry.getInstance().debug)                
                ex.printStackTrace();
      }
    }

    @Override
    public void onDisconnect() {
      //System.out.println("[Qiwi] Socket disconnected");
      this.displayMessage("Соединение с сервером прервано.");
      this.showConnectionOff();
      Registry.getInstance().terminal_id  = -1;
      Registry.getInstance().socket_connected = false;
    }
    
    // Обработка событий от сокета
    public void processEvent(String event, JSONObject... data)
    {
        try
        {
            switch(event)
            {
                case "begin":
                    if (data.length >=0 && data[0].has("lastId")){
                        String last_id = data[0].get("lastId").toString();
                        Registry.getInstance().window.appendTextToLogArea("Получен номер последней транзакции от сервера ( №" + last_id + " )");
                        Registry.getInstance().last_id = Long.parseLong(last_id);
                    } else {
                        Registry.getInstance().window.appendTextToLogArea("Во время получения номера последней транзакции произошла ошибка");
                        Log.getInstance().write("socket_callback", "error while getting last id", data.toString());
                        Registry.getInstance().last_id = 0;
                    }                    
                   
                    break;
                case "success":
                    //{"success":" 54"}
                    for(int i = 0; i < data.length; i++)
                    {
                        JSONObject o = data[i];
                        
                        if ( ! o.isNull("id"))
                            if ( ! Local.getInstance().markTransactionAsExecuted(o.getString("id"))){
                                Registry.getInstance().window.appendTextToLogArea("Транзакция №" + o.getString("id") + " зарегистрирована в АРМ");
                                this.displayMessage("Платеж №" + o.getString("id") + " зарегистрирован в АРМ. Необходимо привязать к нему продавца.");
                            }                                
                            else
                                Log.getInstance().write("socket_callback", "error while marking transaction №", o.getString("id"));//System.out.println("[Qiwi] Error while marking transaction №" + o.getString("id"));
                        else
                            Log.getInstance().write("socket_callback", "corrupted JSON from server", o.toString());//System.out.println("[Qiwi] Corrupted JSON ( " + o.toString() + " )");                       
                    }
                    break;
            }
        }
        catch(Exception ex)
        {
            Log.getInstance().write("socket_callback", "error while processing event", ex.toString());                    
            if ( Registry.getInstance().debug)                
                ex.printStackTrace();
        }
    }
    
    private void displayMessage(String message){
        if (Registry.getInstance().window != null){
            Registry.getInstance().window.displayMessage(message);
        }
    }
    
    private void showConnectionOn(){
        if (Registry.getInstance().window != null){
            Registry.getInstance().window.showConnectionOn();
        }
    }
    
    private void showConnectionOff(){
        if (Registry.getInstance().window != null){
            Registry.getInstance().window.showConnectionOff();
        }
    }
}

package ru.erhaben.qiwi.network;

import com.clwillingham.socket.io.IOSocket;
import org.json.JSONObject;
import ru.erhaben.qiwi.misc.Registry;

public class Socket
{
    private static volatile IOSocket socket = null;
    
    private Socket(){
        
    }
    
    public static void send(String message) throws Exception
    {
        init();
        socket.send(message);
    }
    
    // Отправка сокетом события на сервер
    public static void emit(String event, JSONObject data) throws Exception
    {
        init();
        socket.emit(event, data);
    }   
    
    // Инициализация сокета, попытка подключиться к серверу.
    public static void init()
    {
        //Registry.getInstance().window.appendTextToLogArea("Проверка подключкния");
        if ( socket == null )
        {
            //System.out.println("[Qiwi] Socket is null");
            synchronized (Socket.class)
            {
                if ( socket == null )
                {
                    socket = new IOSocket(Registry.getInstance().SOCKET_URL, new SocketCallback());

                    socket.connect();
                }
            }
        }
        else if ( ! socket.isConnected())
        {
            //System.out.println("[Qiwi] Socket isnt connected");
            socket.connect();
        }          
    }
}

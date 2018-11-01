package master;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;


public class MasterServer
{
    private ServerSocket serverCommandSocket;
    public static final int DEFAULT_SERVER_COMMAND_PORT = 4444;
    public static final int DEFAULT_SERVER_DATA_PORT = 4445;

    /**
     * The constructor of MasterServer
     * @param commandPort The chosen port for command flow
     * @param dataPort The chosen port for data flow
     */

    public MasterServer(int commandPort, int dataPort)
    {


        try
        {
            serverCommandSocket = new ServerSocket(commandPort);
            System.out.println("Master opened up 2 server socket on " + Inet4Address.getLocalHost());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("master.MasterServer class.Constructor exception on opening a server socket on Master");
        }
        while (true)
        {
            ListenAndAcceptFromFollowers();
        }
    }

    /**
     * Listens to the port and creates a new CommandConnectionThread object on a demand of connection from a follower
     */
    private void ListenAndAcceptFromFollowers()
    {
        Socket commandSocket;
        try
        {
            commandSocket = serverCommandSocket.accept();
            System.out.println("Command connection was established with a follower client on the address of " + commandSocket.getRemoteSocketAddress());
            CommandConnectionThread commandConnectionThread = new CommandConnectionThread(commandSocket, DEFAULT_SERVER_DATA_PORT);
            commandConnectionThread.start();
        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("master.MasterServer Class.Connection establishment error inside listen and accept function");
        }
    }

}
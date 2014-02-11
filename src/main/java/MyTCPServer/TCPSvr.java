/**
 * 
 */
package MyTCPServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import MgrMain.Main;

/**
 * @author Matthew Glennon (mglennon@virginiafirst.org)
 *         https://github.com/VirginiaFIRST/FTC-FieldMgmt
 */
public class TCPSvr {
    final public static Logger          logger          = LoggerFactory.getLogger(Main.class);
    private static      ServerSocket    sSocket         = null;
    private static      serverThread    sThread         = null;
    
    private static       Socket         cSocket         = null;
    private static final int            maxClientsCount = 5;
    private static final clientThread[] threads         = new clientThread[maxClientsCount];
    
    private static boolean                     stopRequested   = false;

    public static void sendToAllClients(final TCPPack pack) {
        for (final clientThread thread : threads) {
            if (thread != null) {
                thread.SendPack(pack);
            }
        }
    }
    public void abort(){
        stopRequested = true;
        for(clientThread thread : threads){
            if(thread != null){
                thread.interrupt();
            }
        }
        try {
            sSocket.close();
            sThread.interrupt();
        } catch (IOException e) {
            logger.error("Unable to close Client Listner");
        }
    }
    public TCPSvr(final int Port) {
        try {
            sSocket = new ServerSocket(Port);
            sThread = new serverThread();
            sThread.start();
        } catch (final IOException e) {
            logger.error("Error Claiming port for Client Listener");
        }


    }

    private class serverThread extends Thread {

        @Override
        public void run() {
            while (!stopRequested) {
                try {
                    cSocket = sSocket.accept();
                    boolean availThread = false;
                    for (clientThread thread : threads) {
                        if (thread == null) {
                            (thread = new clientThread(cSocket, threads)).start();
                            availThread = true;
                            break;
                        }
                    }
                    if (!availThread) {
                        // TODO: Tell the client we don't have space?
                        cSocket.close();
                    }
                } catch (SocketException e){
                    logger.info("Client Listner Socket Aported - We must be quitting!");
                } catch (IOException e) {
                    logger.error("Unhandled IO Exception while accepting client");
                }
            }
        }
    }

    private class clientThread extends Thread {

        private ObjectInputStream    ObjStream    = null;
        private Socket               clientSocket = null;
        private final clientThread[] threads;

        public clientThread(final Socket clientSocket, final clientThread[] threads) {
            this.clientSocket = clientSocket;
            this.threads = threads;
        }

        @Override
        public void run() {
            final clientThread[] threads = this.threads;
            try {
                // Create input and output streams for this client.
                ObjStream = new ObjectInputStream(clientSocket.getInputStream());
                while (!stopRequested) {
                    // Do the magic! ---------------------------------------
                    final Object inObj = ObjStream.readObject();
                    final TCPPack inPack = (TCPPack) inObj;
                    switch (inPack.PackType) {
                        case NONE:

                            break;
                        case REFRESH_REQUEST:

                            break;
                    }
                }
                // Clean up.
                for (clientThread thread : threads) {
                    if (thread == this) {
                        thread = null;
                    }
                }
                // Close up shop.
                ObjStream.close();
                clientSocket.close();
            } catch (ClassNotFoundException e) {
                
            } catch (SocketException e){
                logger.info("Client Connection Aborted!");
            } catch (IOException e) {

            }
        }

        public void SendPack(final TCPPack pack) {
            try {
                final ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.writeObject(pack);
            } catch (final IOException e) {

            }

        }
    }
}
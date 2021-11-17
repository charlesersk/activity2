package server;

import java.net.*;
import java.io.*;
import java.util.*;

import buffers.ResponseProtos;
import org.json.*;
import java.lang.*;

import buffers.RequestProtos.Request;
import buffers.RequestProtos.Logs;
import buffers.RequestProtos.Message;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

class SockBaseServer {
    static String logFilename = "logs.txt";

    ServerSocket serv = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
    Game game;


    public SockBaseServer(Socket sock, Game game){
        this.clientSocket = sock;
        this.game = game;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }
    }

    // Handles the communication right now it just accepts one input and then is done you should make sure the server stays open
    // can handle multiple requests and does not crash when the server crashes
    // you can use this server as based or start a new one if you prefer. 
    public void start() throws IOException {
        String name = "";


        System.out.println("Ready...");
        while (true) {
            try {
                // read the proto object and put into new objct
                Request op = Request.parseDelimitedFrom(in);
                String result = null;



                // if the operation is NAME (so the beginning then say there is a commention and greet the client)
                if (op.getOperationType() == Request.OperationType.NAME) {
                    // get name from proto object
                    name = op.getName();

                    // writing a connect message to the log with name and CONNENCT
                    writeToLog(name, Message.CONNECT);
                    System.out.println("Got a connection and a name: " + name);
                    Response response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.GREETING)
                            .setMessage("Hello " + name + " and welcome. Welcome to a simple game of battleship. ")
                            .build();
                    response.writeDelimitedTo(out);
                }
                else if (op.getOperationType() == Request.OperationType.LEADER) {

                    System.out.println("Requesting leader board: " + name);
                    List<Entry> list = Response.newBuilder()
                            .setResponseType(Response.ResponseType.LEADER)
                            .getLeaderList();
                    final Response response = Response.newBuilder().setResponseType(Response.ResponseType.LEADER).addAllLeader(list).build();
                    response.writeDelimitedTo(out);
                }
                else if (op.getOperationType() == Request.OperationType.NEW) {

                    System.out.println("New User: " + name);
                    final Response response = Response.newBuilder().setResponseType(Response.ResponseType.TASK).setTask("Enter row col values: ").build();
                    response.writeDelimitedTo(out);
                }
                else if (op.getOperationType() == Request.OperationType.ROWCOL) {

                    System.out.println("Row col from: " + name);
                    final int row = op.getRow();
                    final int column = op.getColumn();
                    final int start = game.getIdx();
                    final String image = game.replaceOneCharacter(row, column);
                    final int end = game.getIdx();
                    boolean hit = false;
                    boolean won = false;
                    if (start < end) {
                        hit = true;
                    }
                    if (end == 12) {
                        won = true;
                    }

                    if (won) {
                        final Response response = Response.newBuilder()
                                .setResponseType(Response.ResponseType.WON)
                                .setTask("You Won")
                                .setImage(image)
                                .build();
                        response.writeDelimitedTo(out);
                    } else {
                        final Response response = Response.newBuilder()
                                .setResponseType(Response.ResponseType.TASK)
                                .setHit(hit)
                                .setTask("Select a row and a column")
                                .setImage(image)
                                .build();
                        response.writeDelimitedTo(out);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
//            if (out != null)  out.close();
//            if (in != null)   in.close();
//            if (clientSocket != null) clientSocket.close();
            }
        }
    }


    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect) 
     * @return String of the new hidden image
     */
    public static void writeToLog(String name, Message message){
        try {
            // read old log file 
            Logs.Builder logs = readLogFile();

            // get current time and data
            Date date = java.util.Calendar.getInstance().getTime();
            System.out.println(date);

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date.toString() + ": " +  name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // This is only to show how you can iterate through a Logs object which is a protobuf object
            // which has a repeated field "log"

            for (String log: logsObj.getLogList()){

                System.out.println(log);
            }

            // write to log file
            logsObj.writeTo(output);
        }catch(Exception e){
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public static Logs.Builder readLogFile() throws Exception{
        Logs.Builder logs = Logs.newBuilder();

        try {
            // just read the file and put what is in it into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found.  Creating a new file.");
            return logs;
        }
    }


    public static void main (String args[]) throws Exception {
        Game game = new Game();

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);
        }
        int port = 9099; // default port
        int sleepDelay = 10000; // default delay
        Socket clientSocket = null;
        ServerSocket serv = null;

        try {
            port = Integer.parseInt(args[0]);
            sleepDelay = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }
        try {
            serv = new ServerSocket(port);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        System.out.println("Server Started...");
        clientSocket = serv.accept();
        game.newGame();
        SockBaseServer server = new SockBaseServer(clientSocket, game);
        server.start();

    }
}


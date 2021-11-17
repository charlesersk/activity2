package client;

import java.net.*;
import java.io.*;

import org.json.*;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

import java.util.*;
import java.util.stream.Collectors;

class SockBaseClient {

    public static void main (String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int i1=0, i2=0;
        int port = 9099; // default port
        boolean nonStop = true;

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Ask user for username
        System.out.println("Please provide your name for the server.");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend = stdin.readLine();

        // Build the first request object just including the name
        Request op = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();
        Response response;
        try {
            // connect to the server
            serverSock = new Socket(host, port);

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            op.writeDelimitedTo(out);

            // read from the server
            response = Response.parseDelimitedFrom(in);

            // print the server response. 
            System.out.println(response.getMessage());

            while (nonStop) {
                System.out.println("* \nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game");

                System.out.println("Please enter your option.");
                BufferedReader optionReader = new BufferedReader(new InputStreamReader(System.in));
                String option = optionReader.readLine();

                if ("1".equalsIgnoreCase(option)) {
                    Request request = Request.newBuilder()
                            .setOperationType(Request.OperationType.LEADER).build();
                    request.writeDelimitedTo(out);
                    response = Response.parseDelimitedFrom(in);
                    System.out.println(response.getMessage());
                } else if ("2".equalsIgnoreCase(option)) {
                    boolean play = true;
                    Request request = Request.newBuilder()
                            .setOperationType(Request.OperationType.NEW).build();
                    request.writeDelimitedTo(out);
                    response = Response.parseDelimitedFrom(in);
                    System.out.println(response.getMessage());
                    while (play) {
                        System.out.print("Please enter col value: ");
                        String col = optionReader.readLine();

                        if (col.equalsIgnoreCase("BYE")) {
                            Request quit = Request.newBuilder()
                                    .setOperationType(Request.OperationType.BYE)
                                    .build();
                            quit.writeDelimitedTo(out);
                            response = Response.parseDelimitedFrom(in);
                            System.out.println(response.getMessage());
                            play = false;
                            continue;
                        }

                        int colValue = -1;
                        try {
                            colValue = Integer.parseInt(col);
                        } catch (Exception e) {
                            System.out.println("Value must be int");
                            continue;
                        }

                        System.out.print("Please enter row value: ");
                        String row = optionReader.readLine();
                        int rowValue = -1;
                        try {
                            rowValue = Integer.parseInt(row);
                        } catch (Exception e) {
                            System.out.println("Value must be int");
                            continue;
                        }

                        Request answer = Request.newBuilder()
                                .setOperationType(Request.OperationType.ROWCOL)
                                .setColumn(colValue)
                                .setRow(rowValue)
                                .build();
                        answer.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        System.out.println("\nTask Type: " + response.getResponseType());
                        System.out.println("Image: \n" + response.getImage());
                        System.out.println("Task: \n" + response.getTask());
                        if (response.getHit()) {
                            System.out.println("Yay! A battleship found : " + response.getMessage() + " to go...");
                        } else {
                            System.out.println("You missed!");
                        }
                    }

                } else if ("3".equalsIgnoreCase(option)) {
                    Request request = Request.newBuilder()
                            .setOperationType(Request.OperationType.QUIT).build();
                    request.writeDelimitedTo(out);
                    response = Response.parseDelimitedFrom(in);
                    System.out.println(response.getMessage());
                    nonStop = false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }
}



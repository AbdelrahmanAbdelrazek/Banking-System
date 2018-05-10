package bankingsystem;
import java.net.*;
import java.util.*;

/**
 * class of the bank server
 */

public class BankServer extends Thread{
    
    private String administrator,password,host_database,name,schema;
    private int portnum;
    private ServerSocket server;
    public static volatile Hashtable <String, String[]> allBanks = new Hashtable<String, String[]>();

    /**
     * constructor of the bank server to assign the server to IP address ,port number and its database and give it its privileges
     * 
     * @param ip       IP address of the server
     * @param port     server port number which listen to the incoming connections
     * @param admin    user name of the database administrator
     * @param pass     password of the database administrator
     * @param database address of database
     * @param name     name of the bank
     * @param schema   schema of database tables
     */
    public BankServer(String ip,int port,String admin,String pass,String database,String name,String schema) {
        portnum = port;
        administrator = admin;
        password = pass;
        host_database = database;
        this.schema = schema;
        this.name = name;
        String [] x = {ip,String.valueOf(port)};
        allBanks.put (name,x);
        
    }
    
    public void addBank(String name, String ip, String port){
        String [] x = {ip,String.valueOf(port)};
        allBanks.put (name,x);
    }
    /**
     * run the server to listen to the coming connections  ' for ever '
     */
    @Override
    public void run(){
        try{
         server = new ServerSocket(portnum);
            while(true){
                Socket c = server.accept();
                ClientHandler ch = new ClientHandler(c,administrator,password,host_database,name,schema);
                ch.start();
            }  
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
           
    }
    
    
    
    public static void main(String[] args){
        String bank1_database = "jdbc:derby://localhost:1527/Bank2";
        String bank1_schema="ASIM2";
        BankServer  bank_server_1 ;
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.println("Enter the server name: ");
        String server_name = reader.nextLine();
        System.out.println("Enter the port number: ");
        int port_number = reader.nextInt();
        reader.nextLine(); //consume empty new line
        bank_server_1 = new BankServer("127.0.0.1",port_number,"asim2","asim2",bank1_database,server_name,bank1_schema);
       System.out.println("Enter the server names, IP and ports of all the other servers one by one:\n"
               + "Example: xxx.xxx.xxx.xxx, xxxx, Bank 1 \n"
                  + "if you want to start the server enter 'start': ");
       String bank_server_info = reader.nextLine();
       while(!bank_server_info.equals("start")){
           String[] details = bank_server_info.split("[,]");
           details[0] = details[0].replaceAll("\\s+","");
           details[1] = details[1].replaceAll("\\s+","");
           details[2] = details[2].trim();
           bank_server_1.addBank(details[2], details[0], details[1]);
           bank_server_info = reader.nextLine();
       }
       System.out.println("Server is running...");
       bank_server_1.start();
    }
}

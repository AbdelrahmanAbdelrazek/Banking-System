package bankingsystem;
import java.net.*;
import java.io.*;
import java.sql.*;

/**
 * class to handle each client on its own where all client handlers run in parallel threads
 * 
 */
class ClientHandler extends Thread {
    private Socket c;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String admin,pass,host_database,accountNum,host_name,database_schema;
    private Connection con;

    /**
     * constructor to assign each client handler to a specific client socket and give it its privileges over its bank database
     * 
     * @param c socket to communicate to its client
     * @param admin username of the database administrator 
     * @param pass  password of the database administrator
     * @param host  address of its bank database
     * @param bank  name of the bank its currently running inside
     * @param sch   schema of the database tables
     */
    ClientHandler(Socket c,String admin,String pass,String host,String bank,String sch) {
        this.c = c;
        this.admin = admin;
        this.pass = pass;
        this.host_database = host;
        this.host_name = bank;
        database_schema = sch;
    }
    
    
     /**
      * authenticate username and password by checking the database 
      * and assign the account number of the logged in client to the local variable accountNum of this client handler
      * 
      * @param un  username provided by client
      * @param ps  password provided by client
      * 
      * @return    0 username and password are correct 
      *            1 username or password is not correct
      */
     private int auth(String un,String ps){
         String sql = "SELECT * FROM "+ database_schema+".CLIENTS WHERE USERNAME = '"+un+"'" ;
         try{
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE , ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(sql) ;
            String check_pass="";
            if(rs.next()){
                check_pass = rs.getString("PASSWORD");
                check_pass = check_pass.replaceAll("\\s+","");
            }else{
                System.out.println("rs failed");
            }
                
            
            if(ps.equals(check_pass)){
                accountNum = rs.getString("ID");
                rs.close();
                stmt.close();
                return 0;
            }else{
                rs.close();
                stmt.close();
                return 1;
            }
         }catch(SQLException err){
             err.printStackTrace();
             return 1;
         }
     }
     
     /**
      * deposit a certain amount to the logged in client balance
      * 
      * @param amount amount of money to deposit
      * 
      * @return 0 deposit is done 
      *         1 any error occurred 
      */
    private synchronized int deposit(double amount){
        double balance = 0;
        amount = (double) Math.round(amount * 100) / 100; //to round amount to 2 decimal points
        if (amount < 0){
            return 1;
        }else{
         String sql = "SELECT * FROM "+ database_schema+".CLIENTS WHERE ID = "+accountNum +" " ;
         try{
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE , ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(sql) ;
            if(rs.next()){
               balance = rs.getDouble("BALANCE");
               balance += amount;
               balance = (double) Math.round(balance * 100) / 100; //to round balance to 2 decimal points
               sql = "UPDATE "+database_schema+".CLIENTS SET BALANCE = "+balance+" WHERE ID = "+accountNum +" ";
            }else{
                System.out.println("rs failed");
                rs.close();
                stmt.close();
                return 1;
            }
            if(stmt.executeUpdate(sql)!= 0){
                java.util.Date date = new java.util.Date();
                Timestamp sqlDate = new Timestamp(date.getTime());
                sql = "INSERT INTO "+database_schema+".TRANSACTIONHST (ACCOUNTNUM,DATE,AMOUNT,BALANCE,OPERATION) VALUES ( "+ accountNum +" , '"+ sqlDate +"' , "+ amount +" , "+ balance + " , 'DEP') ";
                stmt.executeUpdate(sql);
                rs.close();
                stmt.close();
                return 0;
            }else{
                return 1;
            }
            
         }catch(SQLException err){
             System.out.println( err.getMessage( ) );
             return 1;
         }
    }
 }
    
    /**
     * withdraw a certain amount from the logged in client balance
     * 
     * @param amount amount of money to withdraw
     * 
     * @return 0 withdraw done
     *         1 failed 
     *         2 balance is not enough
     */
    private synchronized int wtd(double amount){
        double balance = 0;
        amount = (double) Math.round(amount * 100) / 100; //to round amount to 2 decimal points
        if (amount < 0){
            return 1;
        }else{
         String sql = "SELECT * FROM "+database_schema+".CLIENTS WHERE ID = "+accountNum +" " ;
         try{
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE , ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(sql) ;
            if(rs.next()){
               balance = rs.getDouble("BALANCE");
               if(amount > balance){
                   rs.close();
                   stmt.close();
                   return 2;
               }else{
                   balance -= amount;
                   balance = (double) Math.round(balance * 100) / 100; //to round balance to 2 decimal points
               }
               sql = "UPDATE "+database_schema+".CLIENTS SET BALANCE = "+balance+" WHERE ID = "+accountNum +" ";
            }else{
                System.out.println("rs failed");
                return 1;
            }
            if(stmt.executeUpdate(sql)!= 0){
                java.util.Date date = new java.util.Date();
                Timestamp sqlDate = new Timestamp(date.getTime());
                sql = "INSERT INTO "+database_schema+".TRANSACTIONHST (ACCOUNTNUM,DATE,AMOUNT,BALANCE,OPERATION) VALUES ( "+ accountNum +" , '"+ sqlDate +"' , "+ amount +" , "+ balance + " , 'WTD') ";
                stmt.executeUpdate(sql);
                rs.close();
                stmt.close();
                return 0;
            }else{
                rs.close();
                stmt.close();
                return 1;
            }
            
         }catch(SQLException err){
             System.out.println( err.getMessage( ) );
             return 1;
         }
        }
    }
    
    /**
     * transfer a certain amount to another account either in the same bank or another bank
     * 
     * @param bank_name name of the bank to which the transfer is going
     * @param acc_num   number of account to which the transfer is going 
     * @param amount    amount of transfered money 
     * 
     * @return 0 transfer is done
     *         1 transfer failed
     *         2 balance is not enough
     *         3 bank name is wrong
     *         4 account number not found
     */
    private synchronized int transfer(String bank_name, int acc_num, double amount){
        amount = (double) Math.round(amount * 100) / 100; //to round amount to 2 decimal points
            String [] address = BankServer.allBanks.get (bank_name);
            if (address == null ){
                return 3;
            }else{
                String sql = "SELECT * FROM "+database_schema+".CLIENTS WHERE ID = "+accountNum +" " ;
                try{
                   Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE , ResultSet.CONCUR_READ_ONLY);
                   ResultSet rs = stmt.executeQuery(sql) ;
                   if(rs.next()){
                        double balance = rs.getDouble("BALANCE");
                        if(amount > balance){
                            rs.close();
                            stmt.close();
                            return 2;
                        }else{
                            if(bank_name.equals(this.host_name)){
                                if(acc_num == Integer.parseInt(this.accountNum)){
                                    rs.close();
                                    stmt.close();
                                    return 1;
                                }
                               int done =  this.transferIN(acc_num, amount, Integer.parseInt(this.accountNum), this.host_name);
                               switch (done){
                                        case 0:
                                           balance -= amount;
                                           balance = (double) Math.round(balance * 100) / 100; //to round balance to 2 decimal points
                                           sql = "UPDATE "+database_schema+".CLIENTS SET BALANCE = "+balance+" WHERE ID = "+accountNum +" ";
                                           stmt.executeUpdate(sql);
                                           java.util.Date date = new java.util.Date();
                                           Timestamp sqlDate = new Timestamp(date.getTime());
                                           sql = "INSERT INTO "+database_schema+".TRANSACTIONHST (ACCOUNTNUM,DATE,AMOUNT,BALANCE,OPERATION,BANKNAME,TOACCOUNT) VALUES ( "+ accountNum +" , '"+ sqlDate +"' , "+ amount +" , "+ balance + " , 'TRFOUT', ' "+ bank_name+" ' , "+ acc_num +" ) ";
                                           stmt.executeUpdate(sql);
                                           rs.close();
                                           stmt.close();
                                           return 0;

                                        case 4:
                                           rs.close();
                                           stmt.close();                                            
                                            return 4;
                                        default:
                                           rs.close();
                                           stmt.close();                                            
                                            return 1;
                                    }
                            }else{
                                try{
                                        Socket client = new Socket(address[0], Integer.parseInt(address[1]));
                                        DataInputStream client_dis  = new DataInputStream(client.getInputStream());
                                        DataOutputStream client_dos = new DataOutputStream(client.getOutputStream()); 
                                        client_dos.writeUTF("TRFREQ:"+acc_num+","+amount+","+accountNum+","+host_name);
                                        System.out.println("Sending ==>\t" + "TRFREQ:"+acc_num+","+amount+","+accountNum+","+host_name);
                                        String done = client_dis.readUTF();
                                        System.out.println("Recieved ==>\t" + done);
                                        switch (done){
                                            case "0":
                                               balance -= amount; 
                                               balance = (double) Math.round(balance * 100) / 100; //to round balance to 2 decimal points
                                               
                                               sql = "UPDATE "+database_schema+".CLIENTS SET BALANCE = "+balance+" WHERE ID = "+accountNum +" ";
                                               stmt.executeUpdate(sql);
                                               java.util.Date date = new java.util.Date();
                                               Timestamp sqlDate = new Timestamp(date.getTime());
                                               sql = "INSERT INTO "+database_schema+".TRANSACTIONHST (ACCOUNTNUM,DATE,AMOUNT,BALANCE,OPERATION,BANKNAME,TOACCOUNT) VALUES ( "+ accountNum +" , '"+ sqlDate +"' , "+ amount +" , "+ balance + " , 'TRFOUT', ' "+ bank_name+" ' , "+ acc_num +" ) ";
                                               stmt.executeUpdate(sql);
                                                rs.close();
                                                stmt.close();                                               
                                               return 0;

                                            case "4":
                                                 rs.close();
                                                 stmt.close();
                                                return 4;
                                            default:
                                                 rs.close();
                                                 stmt.close();                                                
                                                return 1;
                                        }

                                }catch(Exception e){
                                    System.out.println(e.getMessage());
                                    return 1;
                                }  
                            }
                        }
                        
                    }else {return 4;}
                   
                }catch(SQLException err){
                    System.out.println( err.getMessage( ) );
                    return 1;
                }
       }
  
}
    
    /**
     * get all the information of the logged in account from clients table in the database
     * 
     * @return string of all the information (ACCountNo,firstname,lastname,username,email,balance)
     */
    private String info(){
          String sql = "SELECT * FROM "+database_schema+".CLIENTS WHERE ID = "+accountNum +" " ;
          String record = "";
          try{
Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE , ResultSet.CONCUR_READ_ONLY);
          ResultSet rs = stmt.executeQuery(sql);
          if(rs.next()){   
            record = rs.getString("ID") + ',' +
                        rs.getString("FIRSTNAME") + ',' +
                        rs.getString("LASTNAME") + ',' +
                        rs.getString("USERNAME") + ',' +
                        rs.getString("EMAIL") + ',' +
                        rs.getString("BALANCE");  
          }
            rs.close();
            stmt.close();
            return record.replaceAll("\\s+","");
          }catch(SQLException err){
             System.out.println( err.getMessage( ) );
             return "Not found";
          }
          
    }
    
    /**
     * get the history of the transactions made by the logged in account
     * 
     * @param offset the amount of records to skip to get older transactions
     * 
     * @return string of all the transaction records separated by # (timestamp,operation,bank,accNo,amount,balance)
     */
    private String history(int offset){
        int count = 5;
        String history="",record;
        String sql = "SELECT * FROM "+database_schema+".TRANSACTIONHST WHERE ACCOUNTNUM = "+accountNum +" ORDER BY DATE DESC " ;
         try{
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE , ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(sql) ;
            if(!rs.absolute(offset)){
                rs.close();
                stmt.close();
                return "end";
            }
            do{

                history += rs.getTimestamp("DATE");
                record  =  rs.getString("OPERATION");
                record  = record.replaceAll("\\s+","");
                history += "," + record;

                switch (record){
                    case "TRFOUT":
                        record  =  rs.getString("BANKNAME");
                        record  = record.replaceAll("\\s+","");
                        history += "," + record;
                        history += "," + rs.getInt("TOACCOUNT");
                        break;
                    case "TRFIN":
                        record  =  rs.getString("BANKNAME");
                        record  = record.replaceAll("\\s+","");
                        history += "," + record;
                        history += "," + rs.getInt("FROMACCOUNT");
                        break;

                }

                history += "," + rs.getDouble("AMOUNT");
                history += "," + rs.getDouble("BALANCE");
                count--;
                if(count > 0)
                    history += "#";
                else{
                    rs.close();
                    stmt.close();
                    return history;
                }
              }while((rs.next()));
            rs.close();
            stmt.close();       
            return history + "end";
              
         }catch(SQLException err){
             System.out.println( err.getMessage( ) );
             return "SQL ERROR";
          }
    }
    
    /**
     * function to get transfers from the other bank servers
     * 
     * @param toAccount account number to which the transfer is going
     * @param amount    amount of transfered money
     * @param fromAcc   account number from which the transfer is coming
     * @param fromBank  name of the bank from which the transfer is coming
     * 
     * @return 0 transfer is done
     *         1 transfer failed from SQL error
     *         4 account not found
     */
    private synchronized int transferIN(int toAccount,double amount,int fromAcc,String fromBank){
        double balance =0;
        amount = (double) Math.round(amount * 100) / 100; //to round amount to 2 decimal points
        String sql = "SELECT * FROM "+ database_schema+".CLIENTS WHERE ID = "+toAccount +" " ;
        try{
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE , ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(sql) ;
            if(rs.next()){
               balance = rs.getDouble("BALANCE");
               balance += amount;
               balance = (double) Math.round(balance * 100) / 100; //to round balance to 2 decimal points
               sql = "UPDATE "+database_schema+".CLIENTS SET BALANCE = "+balance+" WHERE ID = "+toAccount +" ";
            }else{
                return 4;
            }
                if(stmt.executeUpdate(sql) != 0){
                    java.util.Date date = new java.util.Date();
                    Timestamp sqlDate = new Timestamp(date.getTime());
                    sql = "INSERT INTO "+database_schema+".TRANSACTIONHST (ACCOUNTNUM,DATE,AMOUNT,BALANCE,OPERATION,FROMACCOUNT,BANKNAME) VALUES ( "+ toAccount +" , '"+ sqlDate +"' , "+ amount +" , "+ balance + " , 'TRFIN',"+fromAcc+" ,' "+fromBank+" ' )" ;
                    stmt.executeUpdate(sql);
                    rs.close();
                    stmt.close();
                    return 0;
                }else{
                    rs.close();
                    stmt.close();
                    return 1;
                }
         }catch(SQLException err){
             err.printStackTrace();
             return 1;
         }
    }
    
    /**
     * run the client handler by creating input and output streams with the client and wait for its requests
     */
    @Override
    public void run() {
        
     
        try {
            dis = new DataInputStream(c.getInputStream());
            dos = new DataOutputStream(c.getOutputStream());
           //String sql = "INSERT INTO APP.CLIENTS ( ID,USERNAME,PASSWORD,BALANCE) VALUES ( 2,' "+ us2+ " ',' " + ps2 + " ',100)";
            try {
                con = DriverManager.getConnection(host_database,admin,pass);
                
             // ResultSet.TYPE_SCROLL_INSENSITIVE , ResultSet.CONCUR_READ_ONLY 
            }
            catch ( SQLException err ) {
                System.out.println( err.getMessage( ) );
            }
            boolean out = false;
            while(!out){
                
            String request = dis.readUTF();
            System.out.println("Recieved ==>\t" + request);
            String[] details = request.split("[:,]");
          
            switch (details[0]){
                case "LOG":    int log = this.auth(details[1], details[2]);
                               dos.writeUTF("LOG:"+log);
                               System.out.println("Sending ==>\tLOG:" + log);
                               break;
                               
                case "DEP":    int dep = this.deposit(Double.parseDouble(details[1]));
                               dos.writeUTF("DEP:"+dep);
                               System.out.println("Sending ==>\tDEP:"+dep);
                               break;
                               
                case "WTD":    int wtd = this.wtd(Double.parseDouble(details[1]));
                               dos.writeUTF("WTD:"+wtd);
                               System.out.println("Sending ==>\tWTD:"+wtd);
                               break;
                               
                case "TRF":    int trf = this.transfer(details[1],Integer.parseInt(details[2]),Double.parseDouble(details[3]));
                               dos.writeUTF("TRF:"+trf);
                               System.out.println("Sending ==>\tTRF:"+trf);
                               break;
                               
                case "INF":    String info = this.info();
                               dos.writeUTF("INF:"+info+","+this.host_name);
                               System.out.println("Sending ==>\tINF:"+info);
                               break;
                               
                case "HST":    String hst = this.history(Integer.parseInt(details[1]));
                               dos.writeUTF("HST:"+hst);
                               System.out.println("Sending ==>\tHST:"+hst);
                               break;
                               
                case "TRFREQ": int exist = this.transferIN(Integer.parseInt(details[1]),Double.parseDouble(details[2]),Integer.parseInt(details[3]),details[4]);
                               dos.writeUTF(""+exist);
                               System.out.println("Sending ==>\t"+exist);
                               break;
                               
                case "LGO":    out = true;
                               break;
                }      
            }
           
            c.close();
            dis.close();
            dos.close();
            con.close();
        }catch (IOException e){
            System.out.println(e.getMessage());
        }catch(SQLException sqle){
            sqle.printStackTrace();
        }

    }

}
Recreating the derby database using ij tool:
==========================================
1 Install JDK.
2 run C:\Program Files\Java\'jdk_version'\db\bin\startNetworkServer.bat as an administrator
3.Open CMD as an adminstrator
4.run 'java -jar C:\Program Files\Java\'jdk_version'\db\lib\derbyrun.jar' ij
5.Connect and create database
	ij> connect 'jdbc:derby://localhost:1527/Bank2; create=true;user=asim2;password=asim2';
6. Create Schema
	ij>CREATE SCHEMA ASIM2;
7.Create CLIENTS and TRANSACTIONSHST tables
	ij> CREATE TABLE ASIM2.CLIENTS(
			ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), 
			USERNAME VARCHAR(500), 
			PASSWORD VARCHAR(500), 
			FIRSTNAME VARCHAR(500), 
			LASTNAME VARCHAR(500), 
			EMAIL VARCHAR(500), 
			BALANCE DOUBLE, 
			PRIMARY KEY(ID));
	ij> CREATE TABLE ASIM2.TRANSACTIONHST(
			TRANSACTION_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
			ACCOUNTNUM INT,
			DATE TIMESTAMP,
			AMOUNT DOUBLE, 
			BALANCE DOUBLE, 
			OPERATION VARCHAR(10), 
			BANKNAME VARCHAR(500), 
			TOACCOUNT INT, 
			FROMACCOUNT INT,
			PRIMARY KEY(TRANSACTION_ID));
 8.Insert Clinets into the Clients table:
	Example:
	insert into ASIM2.CLIENTS(USERNAME, PASSWORD, BALANCE, FIRSTNAME, LASTNAME, EMAIL) 
				values('Abdelrahman_90', '1234', 10000, 'Abdelrahman', 'Abdelrazek', 'abdelrahman@gmail.com');

Start Bank(s) Server(s):
=======================
1.Open \BankingSystemServer\dist\Server.bat
2.Follow the instructions (Enter Bank name, port, other banks servers IPs, Ports and Names)

Start Client:
=============
1.Open \BankingSystemClient\dist\BankingSystemClient.jar
2.Enter the IP and port of the Bank you want to connect to.
3.Enter the username and password of the client you want to log in as
4.now you can Import, Export and Transfer money from your account to others.

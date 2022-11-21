package P3;
import java.sql.* ;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class goBabbyApp {

    public static void main (String[] args) throws SQLException
    {
        int sqlCode=0;      // Variable to hold SQLCODE
        String sqlState="00000";  // Variable to hold SQLSTATE

        // Register the driver
        try { DriverManager.registerDriver ( new com.ibm.db2.jcc.DB2Driver() ) ; }
        catch (Exception cnfe){ System.out.println("Class not found"); }

        // This is the url you must use for DB2.
        //Note: This url may not valid now ! Check for the correct year and semester and server name.
        String url = "jdbc:db2://winter2022-comp421.cs.mcgill.ca:50000/cs421";

        //user id, password for login to DB2
        String your_userid = "";
        String your_password = "";
        //AS AN ALTERNATIVE, you can just set your password in the shell environment in the Unix (as shown below) and read it from there.
        //$  export SOCSPASSWD=yoursocspasswd
        if(your_userid == null && (your_userid = System.getenv("SOCSUSER")) == null)
        {
            System.err.println("Error!! do not have a password to connect to the database!");
            System.exit(1);
        }
        if(your_password == null && (your_password = System.getenv("SOCSPASSWD")) == null)
        {
            System.err.println("Error!! do not have a password to connect to the database!");
            System.exit(1);
        }
        Connection con = DriverManager.getConnection (url,your_userid,your_password) ;
        Statement statement = con.createStatement () ;

        // get midwife's practitioner ID
        int flag = 0;
        Scanner sc = new Scanner(System.in);
        String pId = "";

        while (flag == 0) {
            System.out.print("Enter practitioner ID [E] to exit: ");
            pId = sc.nextLine();

            if (pId.equals("E")) {
                con.close();
                return;
            }
            try
            {
                String querySQL = "SELECT pracID from MIDWIVES WHERE pracID = " + "'" + pId + "'";

                java.sql.ResultSet rs = statement.executeQuery ( querySQL ) ;
                while ( rs.next ( ) )
                {
                    String pid = rs.getString ("pracID");
                    if (pId.equals(pid)) {
                        flag = 1;
                    }

                }
                if (flag == 0) {
                    System.out.println("Practitioner ID not found");
                }
                rs.close();


            }
            catch (SQLException e)
            {
                sqlCode = e.getErrorCode(); // Get SQLCODE
                sqlState = e.getSQLState(); // Get SQLSTATE

                System.out.println(e);
            }
        }


        // **ask for appointment date to enter, as long as that date has no appointments **
        /* list all appointments for that date for this midwife, ordered by time
            - have additional column P or B for primary or backup for that pregnancy (for which appointment is for)
            - output has mother's Name and HealthCardNum
        */
        flag = 0;
        Boolean optionFive = false;
        String appDate = "";
        Boolean optionD = false;
        while (flag == 0) {

            if (!optionFive || optionD) {
                optionD = false;
                System.out.print("Enter the date (YYYY-MM-DD) for appointment list [E] to exit: ");
                Scanner scanner = new Scanner(System.in);
                appDate = scanner.nextLine();
                if (appDate.equals("E")) {
                    con.close();
                    return;
                }
            }
            List<String> appMothers = new ArrayList<>();
            try {
                String appDateQuery = "WITH mwInfo(nthpreg, cid, is_primary) AS " +
                        "(SELECT NTHPREG, CID, IS_PRIMARY FROM ASSIGNEDMW amw, SETAPPOINT sa " +
                        "WHERE amw.PRACID = " + "'" + pId + "' " +
                        "AND amw.PRACID = sa.PRACID AND sa.PRACID = " + "'" + pId + "'), " +
                        "appInfo(ATIME, is_primary, cID) AS (SELECT ATIME, IS_PRIMARY, mwI.CID " +
                        "FROM APPOINTMENTS app, mwInfo mwI WHERE app.cid = mwI.cid AND app.NTHPREG = mwI.nthpreg " +
                        "AND app.ADATE = " + "'" + appDate + "') SELECT DISTINCT ATIME, is_primary, MNAME, m.QCHCN " +
                        "FROM appInfo, MOTHERS m, COUPLE c WHERE appInfo.cID = c.CID AND c.QCHCN = m.QCHCN " +
                        "ORDER BY ATIME";

                java.sql.ResultSet rs = statement.executeQuery(appDateQuery);
                int iter = 1;
                while (rs.next()) {
                    // an appointment for the Date has been found; update flag
                    flag = 1;
                    Time appTime = rs.getTime("ATIME");
                    Boolean primary = rs.getBoolean("is_primary");
                    String isPrimary = "";
                    if (primary) {
                        isPrimary = "P";
                    } else {
                        isPrimary = "B";
                    }
                    String motherName = rs.getString("MNAME");
                    String HCN = rs.getString("QCHCN");
                    String finalInfo = appTime + " " + motherName + " " + HCN;
                    System.out.println(iter + ": " + appTime + " " + isPrimary + " " + motherName + " " + HCN);
                    appMothers.add(finalInfo);
                    iter++;

                }
            } catch (SQLException e) {
                sqlCode = e.getErrorCode(); // Get SQLCODE
                sqlState = e.getSQLState(); // Get SQLSTATE

                System.out.println(e);
                flag = 0;
                optionD = true;
                continue;
            }
            // no appointments for the Date
            if (flag == 0) {
                System.out.println("No appointments for the date");
                optionD = true;

                //all appointments for the Date has been found
            } else {
                String appNumber;

                int validOption = 0;
                // keep asking for Appointment Number until a valid Number or Option is entered
                while (validOption == 0) {
                    System.out.println("");

                    //get user input for Appointment Option E, D, or Appointment Number
                    System.out.println("Enter the appointment number you'd like to work on.");
                    System.out.print("\t" + "[E] to exit [D] to go back to another date: ");
                    Scanner sc2 = new Scanner(System.in);
                    appNumber = sc2.nextLine();
                    try {
                        int appNum = Integer.parseInt(appNumber);
                        if (appNum > appMothers.size() || appNum == 0 ) {
                            System.out.println("Invalid option..");
                            continue;
                        }
                        //get the necessary app info from list of appointments
                        String appNumMother = appMothers.get(appNum - 1);
                        String[] arr = appNumMother.split(" ");
                        //keep asking for the option
                        while (true) {
                            System.out.println("For " + arr[1] + " " + arr[2] + " " + arr[3]); System.out.println("");
                            System.out.println("1. Review notes");
                            System.out.println("2. Review tests");
                            System.out.println("3. Add a note");
                            System.out.println("4. Prescribe a test");
                            System.out.println("5. Go back to the appointments."); System.out.println("");
                            System.out.print("Enter your choice: ");
                            Scanner sc3 = new Scanner(System.in);
                            int option = sc3.nextInt();

                            // list ALL NOTES relevant for this pregnancy, in descending order of Date Time
                            // then display the MENU with 5 options again
                            if (option == 1) {
                                String hcn = arr[3];
                                try {
                                    String noteQuery = "WITH mwInfo(nthpreg, cid) AS " +
                                            "(SELECT app.NTHPREG, app.CID FROM ASSIGNEDMW amw, APPOINTMENTS app, COUPLE c " +
                                            "WHERE amw.PRACID = " + "'" + pId + "'" + "AND app.ADATE = " +
                                            "'" + appDate + "'" + " AND c.QCHCN = " + "'" + hcn + "'" +
                                            "AND c.CID = app.CID) " +
                                            "SELECT Distinct ADATE, NTIME, LEFT(OBSERV, 50) OBSERVE " +
                                            "FROM APPOINTMENTS app, mwInfo mwI, NOTES n " +
                                            "WHERE app.NTHPREG = mwI.nthpreg AND app.CID = mwI.cid " +
                                            "AND app.APPOINTID = n.APPOINTID ORDER BY ADATE DESC, NTIME DESC";
                                    java.sql.ResultSet rsNotes = statement.executeQuery(noteQuery);
                                    while (rsNotes.next()) {
                                        Date date = rsNotes.getDate("ADATE");
                                        Time time = rsNotes.getTime("NTIME");
                                        String obs = rsNotes.getString("OBSERVE");
                                        System.out.println(date + " " + time + " " + obs);
                                    }
                                    System.out.println("");
                                    rsNotes.close();
                                } catch (SQLException e) {
                                    sqlCode = e.getErrorCode(); // Get SQLCODE
                                    sqlState = e.getSQLState(); // Get SQLSTATE

                                    System.out.println(e);
                                    continue;
                                }
                            }
                            // list all tests relevant for this pregnancy
                            if (option == 2) {
                                String hcn = arr[3];

                                try {
                                    String testQuery = "WITH mwInfo(nthpreg, cid) AS " +
                                            "(SELECT app.NTHPREG, app.CID FROM ASSIGNEDMW amw, APPOINTMENTS app, COUPLE c " +
                                            "WHERE amw.PRACID = " + "'" + pId + "'" + "AND app.ADATE = " +
                                            "'" + appDate + "'" + " AND c.QCHCN = " + "'" + hcn + "'" +
                                            "AND c.CID = app.CID) " +
                                            "SELECT Distinct PRESCDATE, TESTTYPE, " +
                                            "COALESCE(LEFT(RESULT, 50), 'PENDING') as RESULT " +
                                            "FROM APPOINTMENTS app, mwInfo mwI, TESTS t " +
                                            "WHERE app.NTHPREG = mwI.nthpreg AND app.CID = mwI.cid " +
                                            "AND app.APPOINTID = t.APPOINTID ORDER BY PRESCDATE DESC";
                                    java.sql.ResultSet rsTests = statement.executeQuery(testQuery);
                                    while (rsTests.next()) {
                                        Date date = rsTests.getDate("PRESCDATE");
                                        String ttype = rsTests.getString("TESTTYPE");
                                        String result = rsTests.getString("RESULT");
                                        System.out.println(date + " " + "[" + ttype + "]" + " " + result);
                                    }
                                    System.out.println("");
                                    rsTests.close();
                                } catch (SQLException e) {
                                    sqlCode = e.getErrorCode(); // Get SQLCODE
                                    sqlState = e.getSQLState(); // Get SQLSTATE

                                    System.out.println(e);
                                    continue;
                                }
                            }
                            //user enters an observation
                            if (option == 3) {
                                String time = arr[0];
                                String hcn = arr[3];
                                System.out.print("Type your observation: ");
                                Scanner sc4 = new Scanner(System.in);
                                String userObs = sc4.nextLine();
                                try {
                                    String insertNoteQuery = "INSERT INTO NOTES " +
                                            "(SELECT CURRENT_TIME, APPOINTID, " + "'" + userObs + "' " +
                                            "FROM APPOINTMENTS app, COUPLE c, MOTHERS m " +
                                            "WHERE (app.NTHPREG, app.CID) IN (SELECT NTHPREG, CID FROM ASSIGNEDMW amw " +
                                            "WHERE amw.PRACID = " + "'" + pId + "') AND app.CID = c.CID AND " +
                                            "app.ADATE = " + "'" + appDate + "'" + "AND " +
                                            "c.QCHCN = m.QCHCN AND c.QCHCN = " + "'" + hcn + "' " +
                                            "AND m.QCHCN = " + "'" + hcn + "' " + "AND app.ATIME = " + "'" +
                                            time + "' )";
                                    statement.executeUpdate(insertNoteQuery);
                                } catch (SQLException e) {
                                    sqlCode = e.getErrorCode(); // Get SQLCODE
                                    sqlState = e.getSQLState(); // Get SQLSTATE

                                    System.out.println(e);
                                    continue;
                                }
                            }
                            // user enters type of test
                            // prescription + sample dates = current date
                            if (option == 4) {
                                String time = arr[0];
                                String hcn = arr[3];
                                System.out.print("Enter the type of test: ");
                                Scanner sc4 = new Scanner(System.in);
                                String userTypeTest = sc4.nextLine();
                                System.out.print("Enter testID: ");
                                Scanner sc5 = new Scanner(System.in);
                                String userTestId = sc5.nextLine();
                                System.out.print("Enter techID: ");
                                Scanner sc6 = new Scanner(System.in);
                                String userTechId = sc6.nextLine();
                                try {
                                    String insertTest = "INSERT INTO TESTS " +
                                            "(SELECT " + "'" + userTestId + "', " + "'" + userTechId + "', " + "APPOINTID, " +
                                            "CURRENT_DATE, CURRENT_DATE, " + "'" + userTypeTest + "'," + " NULL, NULL " +
                                            "FROM APPOINTMENTS app, COUPLE c, MOTHERS m " +
                                            "WHERE (app.NTHPREG, app.CID) IN (SELECT NTHPREG, CID FROM ASSIGNEDMW amw " +
                                            "WHERE amw.PRACID = " + "'" + pId + "') AND app.CID = c.CID AND " +
                                            "app.ADATE = " + "'" + appDate + "' " + "AND " +
                                            "c.QCHCN = m.QCHCN AND c.QCHCN = " + "'" + hcn + "' " +
                                            "AND m.QCHCN = " + "'" + hcn + "' " + "AND app.ATIME = " + "'" +
                                            time + "')";
                                    statement.executeUpdate(insertTest);

                                } catch (SQLException e) {
                                    sqlCode = e.getErrorCode(); // Get SQLCODE
                                    sqlState = e.getSQLState(); // Get SQLSTATE

                                    System.out.println(e);
                                    continue;
                                }
                            }
                            if (option == 5) {
                                flag = 0;
                                optionFive = true;
                                validOption = 1;
                                break;
                            }
                        }

                    } catch (final NumberFormatException e) {
                        if (appNumber.equals("E")) {
                            con.close();
                            return;
                        }
                        // go back to menu where it asks for the date
                        if (appNumber.equals("D")) {
                            flag = 0;
                            validOption = 1;
                            optionD = true;
                        }
                        else {
                          System.out.println("Invalid option..");

                        }
                    }
                }

            }

        }





        // Finally close the statement and connection
        statement.close ( ) ;
        con.close ( ) ;
    }
}

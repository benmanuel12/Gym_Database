import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;

public class JavaServer {

    static String endString;

    public static void main(String[] args) {
        final int PORT;
        if (args.length != 1) {
            System.out.println("Usage java echoserver PORT");
            System.exit(1);
        }

        PORT = Integer.parseInt(args[0]);

        try {
            Socket socket = null;
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started");
            System.out.println("Waiting for a client...");

            while (true) {
                socket = serverSocket.accept();
                System.out.println("Client Accepted");

                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                String action = (String) input.readObject();

                switch (action) {
                case "Add": {
                    Booking booking = (Booking) input.readObject();
                    add(booking);
                    output.writeObject(endString);
                    output.flush();
                }
                    break;
                case "Update": {
                    String id = (String) input.readObject();
                    Booking booking = (Booking) input.readObject();
                    update(id, booking);
                    output.writeObject(endString);
                    output.flush();
                }
                    break;
                case "Delete": {
                    String id = (String) input.readObject();
                    delete(id);
                    output.writeObject(endString);
                    output.flush();
                }
                    break;
                case "Filter": {
                    String type = (String) input.readObject();
                    String data = (String) input.readObject();
                    listBookingFiltered(type, data);
                    output.writeObject(endString);
                    output.flush();
                }
                    break;
                case "All": {
                    listAll();
                    output.writeObject(endString);
                    output.flush();
                }
                    break;
                }
                socket.close();
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        } catch (ClassNotFoundException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // add a booking, assuming no clashes
    public static void add(Booking bookingDetails) {
        boolean uniqueID = false;
        boolean existingClient = false;
        boolean existingTrainer = false;
        boolean validTime = false;

        // check for user error
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/gymBookings", "root", "");
            Statement statement = conn.createStatement();

            // is the bookingID new?
            String testBookingID = "SELECT bookingID FROM Bookings;";
            ResultSet r1 = statement.executeQuery(testBookingID);

            ArrayList<String> takenIDs = new ArrayList<String>();
            while (r1.next()) {
                takenIDs.add(r1.getString("bookingID"));
            }

            if (!takenIDs.contains(bookingDetails.getId())) {
                uniqueID = true;
            } else {
                endString = ("Booking ID already used");
            }

            // is the Client ID valid?
            String testClientID = "SELECT clientID FROM Clients;";
            ResultSet r2 = statement.executeQuery(testClientID);

            ArrayList<String> takenClients = new ArrayList<String>();
            while (r2.next()) {
                takenClients.add(r2.getString("clientsID"));
            }

            if (takenClients.contains(bookingDetails.getClientId())) {
                existingClient = true;
            } else {
                endString = ("Nonexistant Client ID");
            }

            // is the Trainer ID valid?
            String testTrainerID = "SELECT trainerID FROM Trainers;";
            ResultSet r3 = statement.executeQuery(testTrainerID);

            ArrayList<String> takenTrainers = new ArrayList<String>();
            while (r3.next()) {
                takenTrainers.add(r3.getString("trainerID"));
            }
            if (takenTrainers.contains(bookingDetails.getTrainerId())) {
                existingTrainer = true;
            } else {
                endString = ("Nonexistant Trainer ID");
            }

            // check for clashes
            String testTime = "SELECT timeStart, timeEnd FROM Bookings WHERE clientID = '"
                    + bookingDetails.getClientId() + "' OR trainerID = '" + bookingDetails.getTrainerId() + "';";
            ResultSet r4 = statement.executeQuery(testTime);
            while (r4.next()) {
                // if newstart is before newend and both are either before the start of the
                // current result or after the end of the current result, set validTime to true,
                // otherwise false
                int newStartTime = Integer.parseInt(bookingDetails.getTimeStart().substring(0, 2));
                int newEndTime = Integer.parseInt(bookingDetails.getTimeEnd().substring(0, 2));
                int currentStartTime = Integer.parseInt(r4.getString("timeStart").substring(0, 2));
                int currentEndTime = Integer.parseInt(r4.getString("timeEnd").substring(0, 2));

                if ((newStartTime < newEndTime)
                        && ((newEndTime < currentStartTime) || (currentEndTime < newStartTime))) {
                    validTime = true;
                    endString = ("Valid Time");
                } else {
                    validTime = false;
                    endString = ("Invalid Time");
                }
            }

            // actually add the booking
            if (uniqueID && existingClient && existingTrainer && validTime) {
                String sql = "INSERT INTO Bookings VALUES ('" + bookingDetails.getId() + "', ' "
                        + bookingDetails.getClientId() + "', '" + bookingDetails.getTrainerId() + "', ' "
                        + bookingDetails.getDate() + "', ' " + bookingDetails.getTimeStart() + "', ' "
                        + bookingDetails.getTimeEnd() + "', '" + bookingDetails.getFocus() + "');";

                int qty = statement.executeUpdate(sql);
                endString = (qty + " records were updated");

                statement.close();
            }
        } catch (SQLException ex) {
            endString = ("SQL error: " + ex.getMessage());
        }
    }

    // ------------ Update a specific booking defined by its id -----------
    // SQL Exception should catch if the id does not exist
    // bookingID must be unique, ignore tuple with given id
    // same rules as creating a booking
    public static void update(String bookingID, Booking bookingDetails) {

        boolean uniqueID = false;
        boolean existingClient = false;
        boolean existingTrainer = false;
        boolean validTime = false;

        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/gymBookings", "root", "");
            Statement statement = conn.createStatement();

            // is the bookingID new?
            String testBookingID = "SELECT bookingID FROM Bookings;";
            ResultSet r1 = statement.executeQuery(testBookingID);

            ArrayList<String> takenIDs = new ArrayList<String>();
            while (r1.next()) {
                takenIDs.add(r1.getString("bookingID"));
            }

            // remove the bookingID of the one they want to change, as if they choose not to
            // change the id it is still a valid change
            takenIDs.remove(takenIDs.indexOf(bookingID));

            if (!takenIDs.contains(bookingDetails.getId())) {
                uniqueID = true;
            } else {
                endString = ("Booking ID already used");
            }

            // is the Client ID valid?
            String testClientID = "SELECT clientID FROM Clients;";
            ResultSet r2 = statement.executeQuery(testClientID);

            ArrayList<String> takenClients = new ArrayList<String>();
            while (r2.next()) {
                takenClients.add(r2.getString("clientsID"));
            }

            if (takenClients.contains(bookingDetails.getClientId())) {
                existingClient = true;
            } else {
                endString = ("Client ID already used");
            }

            // is the Trainer ID valid?
            String testTrainerID = "SELECT trainerID FROM Trainers;";
            ResultSet r3 = statement.executeQuery(testTrainerID);

            ArrayList<String> takenTrainers = new ArrayList<String>();
            while (r3.next()) {
                takenTrainers.add(r3.getString("trainerID"));
            }

            if (takenTrainers.contains(bookingDetails.getTrainerId())) {
                existingTrainer = true;
            } else {
                endString = ("Trainer ID already used");
            }

            // check for clashes
            String testTime = "SELECT timeStart, timeEnd FROM Bookings WHERE clientID = '"
                    + bookingDetails.getClientId() + "' OR trainerID = '" + bookingDetails.getTrainerId() + "';";
            ResultSet r4 = statement.executeQuery(testTime);
            while (r4.next()) {
                // if newstart is before newend and both are either before the start of the
                // current result or after the end of the current result, set validTime to true,
                // otherwise false
                int newStartTime = Integer.parseInt(bookingDetails.getTimeStart().substring(0, 2));
                int newEndTime = Integer.parseInt(bookingDetails.getTimeEnd().substring(0, 2));
                int currentStartTime = Integer.parseInt(r4.getString("timeStart").substring(0, 2));
                int currentEndTime = Integer.parseInt(r4.getString("timeEnd").substring(0, 2));

                if ((newStartTime < newEndTime)
                        && ((newEndTime < currentStartTime) || (currentEndTime < newStartTime))) {
                    validTime = true;
                    endString = ("Valid Time");
                } else {
                    validTime = false;
                    endString = ("Invalid Time");
                }
            }

            if (uniqueID && existingClient && existingTrainer && validTime) {
                String sql = "UPDATE Bookings SET bookingID = '" + bookingDetails.getId() + "', clientID = '"
                        + bookingDetails.getClientId() + "', trainerID = '" + bookingDetails.getTrainerId()
                        + "', dateBooked = '" + bookingDetails.getDate() + "', timeStart = '"
                        + bookingDetails.getTimeStart() + "', timeEnd = '" + bookingDetails.getTimeEnd()
                        + "', focus = '" + bookingDetails.getFocus() + "' WHERE bookingID = '" + bookingID + "';";
                int qty = statement.executeUpdate(sql);
                endString = (qty + " records were updated");

                statement.close();
            }
        } catch (SQLException ex) {
            System.out.println("SQL error: " + ex.getMessage());
        }
    }

    // ---------- Delete a specific booking defined by its id ----------
    // SQL Exception should catch if the id does not exist
    public static void delete(String bookingID) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/gymBookings", "root", "");

            String sql = "DELETE FROM Bookings WHERE bookingID = '" + bookingID + "';";
            Statement statement = conn.createStatement();

            int qty = statement.executeUpdate(sql);
            endString = (qty + " records were updated");

            statement.close();
        } catch (SQLException ex) {
            endString = ("SQL error: " + ex.getMessage());
        }
    }

    // ---------- List all bookings for a given client/trainer/date ----------

    public static void listBookingFiltered(String type, String data) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/gymBookings", "root", "");

            String sql = "SELECT * FROM Bookings WHERE " + type + " = '" + data + "';";
            Statement statement = conn.createStatement();
            ResultSet results = statement.executeQuery(sql);

            while (results.next()) {
                endString = (results.getString("bookingID") + " " + results.getString("clientID") + " "
                        + results.getString("trainerID") + " " + results.getString("dateBooked") + " "
                        + results.getString("timeStart") + " " + results.getString("timeEnd") + " "
                        + results.getString("focus"));
            }

            statement.close();
        } catch (SQLException ex) {
            endString = ("SQL error: " + ex.getMessage());
        }
    }

    // ----------- List all bookings ----------

    public static void listAll() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/gymBookings", "root", "");

            String sql = "SELECT * FROM Bookings;";
            Statement statement = conn.createStatement();
            ResultSet results = statement.executeQuery(sql);

            while (results.next()) {
                endString = (results.getString("bookingID") + " " + results.getString("clientID") + " "
                        + results.getString("trainerID") + " " + results.getString("dateBooked") + " "
                        + results.getString("timeStart") + " " + results.getString("timeEnd") + " "
                        + results.getString("focus"));
            }

            statement.close();
        } catch (SQLException ex) {
            endString = ("SQL error: " + ex.getMessage());
        }
    }
}

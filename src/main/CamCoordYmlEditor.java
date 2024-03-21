//Title: Cam-Coord YML Editor.
//Author: Ciaran MacDermott, C20384993
//Date: 16/03/2024
//
//Description: Used in conjunction with the Cam-Coord app, my Final Year Project,
//and MediaMTX (https://github.com/bluenviron/mediamtx), created by BlueEnviron.
//
//MediaMTX uses a YML file to store its configurations. It includes path variables for cameras on the same network as
//the MediaMTX program. MediaMTX will find cameras from these path variables, and perform an action the user configures for them.
//
//This program takes a username and password as input from the user. 
//The username and password are for the account that the user registers through in the Cam-Coord mobile app.
//This program connects to my Spring Boot REST API running on an Azure Virtual Machine.
//
//It checks the username and password are correct, retrieves the camera entries for the account from the SQL database,
//then adds paths variables to the MediaMTX YML file using the details retrieved. It also includes an FFmpeg command in the path
//variable to run on the camera stream. This publishes it to a remote instance of MediaMTX that will run on the Azure VM.
//
//When this is done, the mediamtx.exe file is run to start publishing the camera streams and allow them to be viewed remotely
//in the Cam-Coord app.
//
package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CamCoordYmlEditor implements ActionListener{
	
	//Java Swing GUI elements.
	JFrame frame;
	JPanel panel;
	JTextField username;
	JPasswordField password;
	
	public CamCoordYmlEditor() {
		
		frame = new JFrame();

	    // Load the Cam-Coord logo image from the project file.
	    ImageIcon imageIcon = new ImageIcon(this.getClass().getResource("/Cam-Coord-LogoSharp.jpg"));
	    // Create JLabel to display the image
	    JLabel imageLabel = new JLabel(imageIcon);
	    imageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Set margins for the image

	    //Add button and text fields.
	    JButton button = new JButton("Add Camera Paths");
	    button.setPreferredSize(new Dimension(170, 40));
	    button.addActionListener(this);

	    username = new HintTextField("Username"); // Use HintTextField instead of JTextField, to indicate what user should enter.
	    username.setPreferredSize(new Dimension(250, 40));
	    username.setMargin(new Insets(5, 10, 5, 100));

	    password = new JPasswordField(""); // Use HintTextField instead of JTextField, to hide password.
	    password.setPreferredSize(new Dimension(250, 40));
	    password.setMargin(new Insets(5, 10, 5, 10));

	    panel = new JPanel();
	    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	    panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 10));
	    
	    //Create a panel to display the image and then center it.
	    JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
	    imagePanel.add(imageLabel);
	    imagePanel.setBackground(Color.WHITE); //Set background colour to white.
	    panel.add(imagePanel);
	    panel.add(Box.createRigidArea(new Dimension(0, 10))); //Add spacing between components
	    panel.add(username);
	    panel.add(Box.createRigidArea(new Dimension(0, 10)));
	    panel.add(password);
	    panel.add(Box.createRigidArea(new Dimension(0, 10)));
	    
	    //Create a panel to hold the button and then center it.
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
	    buttonPanel.add(button);
	    buttonPanel.setBackground(Color.WHITE);
	    panel.add(buttonPanel);

	    //Set background color of JFrame content pane to White.
	    panel.setBackground(Color.WHITE);

	    frame.getContentPane().setBackground(Color.WHITE);
	    frame.setLayout(new FlowLayout(FlowLayout.CENTER));//Set as FlowLayout
	    frame.add(panel, BorderLayout.CENTER);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setTitle("Cam-Coord Path Tool");
	    frame.pack();
	    frame.setVisible(true);
	    frame.setPreferredSize(new Dimension(400, 200));
	}//end CamCoordYmlEditor
	
	
	public static void main(String[] args) {
		
		new CamCoordYmlEditor();
		
	}//end main
	
	
	//Edits the mediamtx.yml configuration file by adding in the camera entries for the account that was used to sign in.
	//The JSON array stores the list of details for the cameras the account manages. It is used to create the path entries.
	//
	public static void editFiles(JSONArray jsonCameraArray) {
		
		String ffmpegCommand = "";	//Stores the FFmpeg command.
		
		//Stores details retrieved from the database record.
		String rtspurl = null;
		String streampath = null;
		String cameraid = null;
		
		//Locate the mediamtx.yml file in the current directory the program is run from.
		//and create a temporary file to write changes to before editing the actual .yml file.
		//This program needs to be run from the same directory that mediamtx.exe and mediamtx.yml are in.
		File inputFile = new File(System.getProperty("user.dir")+"\\mediamtx.yml");
        File tempFile = new File(System.getProperty("user.dir")+"\\temp.txt");

        //BufferedReader and writer for working with the text files.
        BufferedReader reader = null;
        BufferedWriter writer = null;
        
        //Open mediamtx.yml.
        try {
			reader = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "mediamtx.yml not found, make sure it is in the same folder as this program.");
			
		}
        
        //Set the BufferedWriter to work with the temporary file.
        try {
			writer = new BufferedWriter(new FileWriter(tempFile));
		} catch (IOException e) {
			e.printStackTrace();
		}

        //Reader will stop at the paths section by matching with this line.
        String lineToRemove = "paths:";
        String currentLine;
        
        
        //Read through the file and remove any existing path variables.
        try {
        	//Continue until the "paths" section is reached.
			while((currentLine = reader.readLine()) != null) {
				
			    //Get current line and trim any newline when comparing with lineToRemove
			    String trimmedLine = currentLine.trim();
			    
			    if(trimmedLine.equals(lineToRemove)) {
			    	writer.write(currentLine + System.getProperty("line.separator"));
			    	break;
			    }
			    
			    writer.write(currentLine + System.getProperty("line.separator"));
			}//end while
			
			//Close the reader and writer.
			writer.close(); 
	        reader.close();
	        
	        //Replace original file with the temporary one.
	        inputFile.delete();
	        tempFile.renameTo(inputFile);
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
       
        
		//Go through the JSON Array of Camera objects and add each one to the paths section of mediamtx.yml
		for(int currentArrayItem = 0;currentArrayItem<jsonCameraArray.length();currentArrayItem++){
			try {
				
				JSONObject currentCamera = null;
				try {
					//Get details of current object and generate the FFmpeg command.
					//This FFmpeg command is used to publish the stream to the VM MediaMTX instance.
					currentCamera = jsonCameraArray.getJSONObject(currentArrayItem);
					
					cameraid = Integer.toString(currentCamera.getInt("cameraid"));
					rtspurl = currentCamera.getString("rtspurl");
					streampath = currentCamera.getString("streampath");
					
					//Change the rtspurl to rtsps
					ffmpegCommand = "ffmpeg -i rtsps://localhost:8322/cam"+cameraid+" -c copy -f rtsp rtsps://172.166.189.197:8322/cam"+cameraid;
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//Write the new path entry.
			    FileWriter myWriter = new FileWriter("C:\\Users\\PC1\\Desktop\\RTSPServer\\mediamtx.yml", true);
			    myWriter.write("\n  cam"+cameraid+":\n");
			    myWriter.write("    source: "+rtspurl+"\n");
			    myWriter.write("    runOnReady: "+ffmpegCommand+"\n");
			    myWriter.write("    runOnReadyRestart: yes\n");
			    myWriter.close();
			}
			catch (IOException e) {
			    JOptionPane.showMessageDialog(null, "Paths were not added.");
			    e.printStackTrace();
			}
			
		}//end for
		
		//Inform user that paths were added successfully.
		JOptionPane.showMessageDialog(null, "Paths added, you can now view your streams remotely while the MediaMTX app is running.");
	}//end editFiles
	
	
	//Return a list of cameras that the logged in user has added to his account.
	//
	public static JSONArray getCameras(String targetURL) {
		
		//Create a URL object from the REST API URL.
	    URL restURL = null;
	    
		try {
			restURL = new URL(targetURL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		
		//Attempt to connect to the REST API using the URL object.
	    HttpURLConnection restConnection = null;
		try {
			restConnection = (HttpURLConnection) restURL.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	     
	    //Only need to use GET requests with the API.
	    try {
			restConnection.setRequestMethod("GET");
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
	    
	    
	    //Get the reponse code.
	    int responseCode = 0;
		try {
			responseCode = restConnection.getResponseCode();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	    //Create a BufferedReader to read from the connection.
	    BufferedReader restConnectionReader = null;
		try {
			restConnectionReader = new BufferedReader(new InputStreamReader(restConnection.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Used to store input from the restConnectionReader.
	    String inputLine;
	    StringBuffer response = new StringBuffer();
	     
	    //Formatting of input.
	    try {
	    	while ((inputLine = restConnectionReader.readLine()) != null) {
	    		response.append(inputLine.replace("[", "").replace("]", ""));
			}
	    } catch (IOException e) {
	    		e.printStackTrace();
	    }
	    
	    
	    //Close the BufferedReader.
	    try {
			restConnectionReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
 
	    
	    //Read JSON response and add it to the array of cameras.
	    //Format it to separate each camera entry returned.
	    String jsonString = "["+response.toString()+"]";
	    JSONArray responseArray = null;
		try {
			responseArray = new JSONArray(jsonString);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    
		//Return the list of camera objects. These are the cameras belonging to the user that need path entries added.
	    return responseArray;
	    
	}//end getCameras

	
	//When the user clicks on the "Add Entries" button,
	//Check the account details are correct,
	//Retrieve a list of the cameras added to the account,
	//Write the camera path entries to the YML file,
	//Start MediaMTX.
	//
	@Override
	public void actionPerformed(ActionEvent e) {

		//Get username and password from text fields.
		String usernameInput = username.getText().toString();
		String passwordInput = password.getText().toString();

		//Check they aren't empty.
		if(usernameInput.equals("")) {
			JOptionPane.showMessageDialog(frame, "Please enter your username.");
			return;
		}
		
		if(passwordInput.equals("")) {
			JOptionPane.showMessageDialog(frame, "Please enter your password.");
			return;
		}
		
		//Set the userid as 0 for default. 0 is recognised as no account being found.
		int userid = 0;
		
		//1.) Login and record the userid value.
		String restUrl = "http://172.166.189.197:8081/Accounts/getbyusername?username="+usernameInput;
	    URL restUrlObject = null;
	     
		try {
			restUrlObject = new URL(restUrl);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();

		}

		//Create the REST connection.
	    HttpURLConnection restConnection = null;

		try {
			restConnection = (HttpURLConnection) restUrlObject.openConnection();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
	     
	    //Only need to use GET to retrieve records.
	    try {
			restConnection.setRequestMethod("GET");
		} catch (ProtocolException e1) {
			e1.printStackTrace();
		}
	    
	    
	     
	    //Set the response code. No response code means server is down.
	    int responseCode = 0;

		try {
			responseCode = restConnection.getResponseCode();
		} catch (IOException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Couldn't connect to server.");
			return;
		}
		
	    //Requesting the details of the account the user entered, so we need to read the server response.
	    //BufferedReader to read the response from the REST API.
	    BufferedReader restResponseReader = null;
		try {
			restResponseReader = new BufferedReader(new InputStreamReader(restConnection.getInputStream()));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//Stores the current line of the response.
	    String inputLine;
	    StringBuffer response = new StringBuffer();
	    
	    //Format the returned line to get the account details returned.
	    try {
	    	while ((inputLine = restResponseReader.readLine()) != null) {
	    		response.append(inputLine.replace("[", "").replace("]", ""));
			}	
	    } catch (IOException e1) {
	    		e1.printStackTrace();
	    }
	    
	    //Close the reader when finished.
	    try {
			restResponseReader.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	     
		//Stores the account JSON object that was returned.
	    JSONObject accountObject = null;
		try {
			accountObject = new JSONObject(response.toString());
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
    	 
    	 
	    try {
			userid = accountObject.getInt("userid");
			//If the userid = 0, then the account doesn't exist.
			//The API returns 0 for userid for non-existent accounts.
			if(userid == 0) {
				JOptionPane.showMessageDialog(frame, "Account not found.");
				return;
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
	    
	    //Check if the passwords entered was correct.
		try {
			if(!passwordInput.equals(accountObject.getString("password"))) {
				JOptionPane.showMessageDialog(frame, "Incorrect password.");
				return;
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
	    
		
		//2.) Check for all of the camera entries for the user.
		//Call the getCameras method and pass the URL.
		JSONArray jsonArray = getCameras("http://172.166.189.197:8081/Cameras/findall?userid="+userid);
		
		//3.) Edit the mediamtx.yml file to include new paths for all cameras in the user's account.
		editFiles(jsonArray);
		
		//4.) Open mediamtx.exe
		//The following command is the command that is needed to see the contents of the given text file
	     String command_to_playwith =" start "+System.getProperty("user.dir")+"\\mediamtx.exe";
	     try {
	    	 String command = "cmd /c" + " start /B " + command_to_playwith;
	         
	         //Starting the new child process
	         Process childprocess11 = Runtime.getRuntime().exec(command);
	      }
	      catch (Exception e1){
	    	  JOptionPane.showMessageDialog(frame, "Paths added, but mediamtx.exe was not found and couldn't be started.");
			return;
	      }
		
		}//end actionPerformed
	
}//end Main

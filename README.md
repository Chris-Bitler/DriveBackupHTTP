#DriveBackupHTTP
A java program written by Chris Bitler to back up a specified linux directory to google drive as a tar file


##Compilation:
You need to add the json-simple-1.1.1.jar as a dependency/library for this project, as it is used to process the results from google's apis
Alternatively, you can download it from our jenkins: TBA

##Configuration:
You need to get a client_id and client_secret for your project by creating a project with the google developer console.
1. Go to https://console.developers.google.com

2. Look for a link on the page that says 'create project'

3. Give the project a name a name in the 'project name' box, it doesn't matter for this.

4. Hit create

5. From the dashboard page it brings you to, go to the blue box saying 'Use google APIs'.

6. Click enable/manage APIs

7. Find 'Drive API' and click it

8. Hit the enable api button

9. It should tell you that you need to create credentials, hit the 'go to credentials' button

10. On the page you are directed to, hit the 'add credentials' dropdown and select "OAuth 2.0 Client ID"

11. Configure the name on the consent screen as it asks

12. Select 'other' as the application type then name it 'CLI'

13. Copy down the client ID and client secret it gives you into your config file

To configure the specific directory for the backups, you need to go to your google drive in a browser, and copy everything after the / after 'folders' in the URL. For example:

https://drive.google.com/drive/u/0/folders/XYZ123

Copy XYZ123 into the config.json for the directory to backup to.

##Credits:
Json-Simple project for making it possible to easily parse the responses

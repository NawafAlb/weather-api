This is the Group 2 WEATHER API README file
____________________________________________________________________________________________________________________________________

There are 3 APIs that need to be running at the same time. There's a
Class API, a data API and a UI API.
____________________________________________________________________________________________________________________________________

To run these files, run the following commands with Maven through PowerShell
Class API: mvn clean compile dependency:copy-dependencies exec:java"-Dexec.mainClass=com.github.nawafalb.ClassApiServer"

Data API: mvn clean compile dependency:copy-dependencies exec:java "-Dexec.mainClass=com.github.nawafalb.ApiServer" "-Dexec.classpathScope=compile"

UI API: mvn clean compile dependency:copy-dependencies exec:java "-Dexec.mainClass=com.github.nawafalb.uiapi.UiApiServer"

OR you can directly run the files through vs code. 

Start the Api server in Data-api.
Then start the Class server in Class-Api.
Then start the UI server in UI-api.

Once the servers are running, use the following links to access the data.
DATA-API- 
http://localhost:8080/airquality
http://localhost:8080/airquality/1
http://localhost:8080/uv
http://localhost:8080/uv/1

CLASS-API- http://localhost:8081/combined

UI-API- http://localhost:8082/dashboard

API server must be started first then class and then ui

____________________________________________________________________________________________________________________________________

Here is a list of all the API endpoints included in the project
API           PATH               METHOD        PURPOSE
Class API     /health            GET           Health check endpoint for the Class AP
Class API     /combined          GET           Calls Data API to fetch AQI and UV data, processes it, and returns a JSON summary
Data API      /airquality        GET           Returns all Air Quality records
Data API      /airquality/{id}   GET           Returns a single Air Quality record by id
Data API      /uv                GET           Returns all UV records from user_DataUV
Data API      /uv/{id}           GET           Returns a single UV record by id
Data API      /table?name={tableName}          Returns all records from any specified table
Data API      /table/{tableName}/{id}          Returns a single record by ID from any specified table
UI API        /health            GET           Health check endpoint for UI API, returns
UI API        /dashboard         GET           Calls Class API /combined, processes data, and returns a structured dashboard JSON
____________________________________________________________________________________________________________________________________

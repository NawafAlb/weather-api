This is the Group 2 WEATHER API README file
____________________________________________________________________________________________________________________________________

There are 3 APIs that need to be running at the same time. There's a
Class API, a data API and a UI API.
____________________________________________________________________________________________________________________________________

To run these files, run the following commands with Maven through PowerShell
Class API: mvn clean compile dependency:copy-dependencies exec:java "-Dexec.mainClass=com.github.nawafalb.ClassApiServer"

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

In order to properly configure these 3 APIs to Apache APISIX, the following things must be done

1. Make sure that Apache APSIX is installed and running. To do this, go into Git Bash and run

docker ps

Several things will be displayed. Make sure that APISIX is one of them. Look for "apache/apisix:3.14.1-debian"

2. Make sure all 3 APIs are running. Instructions on how to do this have already been presented.

3. Route the Data API to port 9080 with this command in Git Bash.

curl -i http://127.0.0.1:9180/apisix/admin/routes/2 \
-H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" \
-X PUT -d '{
  "uri": "/airquality*",
  "upstream": {
    "type": "roundrobin",
    "nodes": {
      "127.0.0.1:8080": 1
    }
  }
}'

Route the Class API to port 9081 with this command in Git Bash,

curl -i http://127.0.0.1:9180/apisix/admin/routes/3 \
-H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" \
-X PUT -d '{
  "uri": "/combined",
  "upstream": {
    "type": "roundrobin",
    "nodes": {
      "127.0.0.1:8081": 1
    }
  }
}'

And finally, route the UI API to port 9082 with this command in Git Bash.

curl -i http://127.0.0.1:9180/apisix/admin/routes/4 \
-H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" \
-X PUT -d '{
  "uri": "/dashboard",
  "upstream": {
    "type": "roundrobin",
    "nodes": {
      "127.0.0.1:8082": 1
    }
  }
}'

4. All 3 APIs should now be properly configured with APISIX. In order to verify if this was done properly,
see if both the original Backend port and the now configured APISIX port return the same information. For
example, to test if the Data API is properly configured, run the following commands in Git Bash:

curl http://localhost:8080/airquality
curl http://127.0.0.1:9080/airquality

They should both return the same information. If they do, then they've been properly configured.
____________________________________________________________________________________________________________________________________



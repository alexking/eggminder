/**
 *  Eggminder
 *
 *  Author: alexking@me.com
 *  Date: 2014-01-07
 */

// Wink API 
private apiUrl() { "https://winkapi.quirky.com/" }
private apiClientId() { "CLIENTID" }
private apiClientSecret() { "CLIENTSECRET" }

metadata {

    preferences 
    {
    	input "username", "text", title : "Wink Username"
        input "password", "password", title : "Wink Password"
    }

	tiles {
		valueTile("number", "device.number")
        {
        	state("number", label : '${currentValue}', unit : "eggs" )
        }
      
	}
    
	main("number")
    details(["number" ])
    
}

def updated()
{
	// Clear the old access token, in case they are switching accounts 
	state.access_token = null 
}

def poll() {

	// We need an access token 
	if (!state.access_token)
	{
		login(); 
    }
    
    // We need an eggtray_id
    if (!state.eggtray_id)
    {
        apiGet("/users/me/wink_devices") { response ->
            
            response.data.data.each() {
              
              	if (it.eggtray_id)
                {
                    log.debug "Found eggtray #" + it.eggtray_id
                	state.eggtray_id = it.eggtray_id
                }
             
            }

		}
    }
    
    // We need to get an eggtray update 
    apiGet("/eggtrays/" + state.eggtray_id) { response ->
    
    	def data = response.data.data

		int numberOfEggs = 0; 
        data.eggs.each() {
            
            if (it)
            {
            	numberOfEggs ++
            }
            
        }
        
        // Only update if there was a change 
        if (device.currentValue('number') != numberOfEggs)
        {
    		sendEvent( name : "number", value : numberOfEggs , unit : "eggs" )
        }
    
    }


}

def apiGet(String path, Closure callback)
{
	httpGet(
    	[ 
    		uri : apiUrl(),
            path : path,
            headers : [
                'Authorization' : 'Bearer ' + state.access_token
            ]
        ], 
    ) {
    	response ->        
        	callback.call(response)
    }
}

def login()
{
            
    httpPostJson(
    	[ 
    		uri : apiUrl(),
            path : "/oauth2/token",
            body : [
    			"client_id": apiClientId(),
    			"client_secret": apiClientSecret(),
    			"username": username,
    			"password": password,
    			"grant_type": "password"
			]
        ], 
        
    ) { response -> 
    
    	state.refresh_token = response.data.data.refresh_token
        state.access_token  = response.data.data.access_token 

    }
    
     
}

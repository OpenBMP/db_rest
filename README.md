DB_REST - OpenBMP Database REST Service
---------------------------------------

Simple DB REST interface to the OpenBMP database. This is an example of how to interact with the database. 

<div style="color:blue; font-size:32px">Demo server is still in development, please bare with it.</div>


JSON
----

All queries have the following JSON syntax/output:

```
{ <table_name> : {
    	cols: <number of columns>,
 		data: [ { <column name>:<value>, ... }, 
 		        ... 
 		      ],
		size: <total number of rows in result>,
        queryTime_ms: <ms value - the duration in ms>,
        fetchTime_ms: <ms value - the duration in ms>
        }
}
```

REST API's
----------
Below documentations the various REST URL/API's. 

*BASE URL* is always **/db_rest/v1**

*DEMO Server Base URL* is **http://demo.openbmp.org:8001/db_rest/v1**

### Routers
BMP Router view.

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/routers | **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns the list of BMP routers currently known in the database | [Demo](http://demo.openbmp.org:8001/db_rest/v1/routers)
GET | /db_rest/v1/routers/status/count | | Returns a count of DOWN and UP BMP routers | [Demo](http://demo.openbmp.org:8001/db_rest/v1/routers/status/count)
GET | /db_rest/v1/routers/status/up | | Returns the BMP routers that are up | [Demo](http://demo.openbmp.org:8001/db_rest/v1/routers/status/up)
GET | /db_rest/v1/routers/status/down | | Returns the BMP routers that are down | [Demo](http://demo.openbmp.org:8001/db_rest/v1/routers/status/down)

### BGP Peers
BGP Peer view. 

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/peer | **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of BGP peers | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer)
GET | /db_rest/v1/peer/localip/{localIP} | **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of BGP peers by local IP address | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/localip/10.22.165.141)
GET | /db_rest/v1/peer/remoteip/{remoteIP} | **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of BGP peers by remote/peer IP address | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/remoteip/172.20.164.43)
GET | /db_rest/v1/peer/asn/{Peer ASN} | **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of BGP peers by Peer ASN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/asn/64555)
GET | /db_rest/v1/peer/type/count | | Returns peer counts by status IP version type | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/type/count)
GET | /db_rest/v1/peer/type/v4 | | Returns list of IPv4 peers  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/type/v4)
GET | /db_rest/v1/peer/type/v6 | | Returns list of IPv6 peers  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/type/v6)
GET | /db_rest/v1/peer/status/count | | Returns peer counts by peer status  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/status/count)
GET | /db_rest/v1/peer/status/up | | Returns a list of BGP peers that are UP | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/status/up)
GET | /db_rest/v1/peer/status/down | | Returns a list of BGP peers that are DOWN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/status/down)
GET | /db_rest/v1/peer/prefix | | Returns a list of BGP peers and their Pre-Policy and Post-Policy RIB counts. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/prefix)
GET | /db_rest/v1/peer/prefix/{peerIP} | | Returns a list of BGP peers and their Pre-Policy and Post-Policy RIB counts by peer address | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/prefix/172.20.164.43)

### RIB
Unicast IPv4/IPv6 RIB/routes view. 

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/rib |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns the RIB entries.  Default limit is 1000.  Should use WHERE clause to filter or use one of the below URI's instead.| [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib?where=PrefixLen >= 8 and PrefixLen <= 9)
GET | /db_rest/v1/rib/asn/{ASN} |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of prefixes by Origin ASN.  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/asn/16509)
GET | /db_rest/v1/rib/asn/{ASN}/count |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of prefixes by Origin ASN.  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/asn/16509/count)
GET | /db_rest/v1/rib/prefix/{Prefix} |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of prefixes by Prefix starts with.  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/prefix/216.40)
GET | /db_rest/v1/rib/prefix/{Prefix}/{length} |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of prefixes by Prefix/Length.  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/prefix/216.40.0.0/20)
GET | /db_rest/v1/rib/history/{prefix}/{length} |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause <br>**days** number of days to include in search.| Returns the RIB history for a given prefix/length.  Default limit is 1000.  Can use WHERE clause to filter on PeerName to restrict to a specific peer. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/history/176.105.176.0/24)

### Upstream BGP ASN's (peers)
List the upstream ASN's for the given ASN.  This query simply looks at the AS PATH and parses out the LEFT most ASN next to the ASN provided.

> ### For example
> AS PATH 1 = 100 200 **300** *400* 500 600 700<br>
> AS PATH 2 = 40 50 60 **70** *400* 888 600 700<br>
> A query on ASN = **400** returns { **300**, **70** } since both of those ASN's are the left most ASN's next to ASN 400.

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET |  /db_rest/v1/upstream/{asn} | | List all upstream ASN's for the given ASN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/upstream/16509)
GET |  /db_rest/v1/upstream/{asn}/count | | List all upstream ASN's for the given ASN and count the number of distinct prefixes (each prefix/len + peer is counted) | [Demo](http://demo.openbmp.org:8001/db_rest/v1/upstream/16509/count)
GET |  /db_rest/v1/upstream/{asn}/peer/count | | List all upstream ASN's for the given ASN and count the number of distinct prefixes per peer | [Demo](http://demo.openbmp.org:8001/db_rest/v1/upstream/16509/peer/count)

### Downstream BGP ASN's (peers)
List the downstream ASN's for the given ASN.  This query simply looks at the AS PATH and parses out the RIGHT most ASN next to the ASN provided.  This will return a an empty ASN which is for itself. 

> ### For example
> AS PATH 1 = 100 200 300 *400* **500** 600 700<br>
> AS PATH 2 = 40 50 60 70 *400* **888** 600 700<br>
> A query on ASN = **400** returns { **500**, **888** } since both of those ASN's are the right most ASN's next to ASN 400.
> 

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET |  /db_rest/v1/downstream/{asn} | | List all downstream ASN's for the given ASN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/downstream/16509')
GET |  /db_rest/v1/downstream/{asn}/count | | List all downstream ASN's for the given ASN and count the number of distinct prefixes (each prefix/len + peer is counted) | [Demo](http://demo.openbmp.org:8001/db_rest/v1/downstream/16509/count)
GET |  /db_rest/v1/downstream/{asn}/peer/count | | List all downstream ASN's for the given ASN and count the number of distinct prefixes per peer | [Demo](http://demo.openbmp.org:8001/db_rest/v1/downstream/16509/peer/count)

### Updates Over Time
Each update is logged.  Attributes that vary by router/peer/prefix will get an update log entry.

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/updates/top | **limit** - Max results to return<br> **hours** - Number of hours to include in top | Return the top prefixes by number of updates in past 'hours'.   Limit defaults to 25 and hours defaults to 2. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/updates/top?limit=15)
GET | /db_rest/v1/updates/top/interval/{minutes} | **minutes** - Interval in minutes<br> **limit** - Max results to return<br> **hours** - Number of hours to include in top | Return the count of updates per interval for up to max limit size. Limit defaults to 25. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/updates/top/interval/5)



### Withdrawns Over Time
Each prefix withdrawn is logged.  

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/withdrawns/top | **limit** - Max results to return<br> **hours** - Number of hours to include in top | Return the top prefixes by number of prefixes withdrawn in past 'hours'.   Limit defaults to 25 and hours defaults to 2. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/withdrawns/top?limit=15)
GET | /db_rest/v1/withdrawns/top/interval/{minutes} | **minutes** - Interval in minutes<br> **limit** - Max results to return<br> **hours** - Number of hours to include in top | Return the count of updates per interval for up to max limit size. Limit defaults to 25. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/withdrawns/top/interval/5)

### AS Stastistics
Per ASN statistics

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET |  /db_rest/v1/asn_stats |  **limit** - Max results to return<br> | Returns a list of all active ASN's in the DB | [Demo](http://demo.openbmp.org:8001/db_rest/v1/as_stats)
GET | /db_rest/v1/asn_stats/{asn} |  **limit** - Max results to return<br> | Returns the AS statistics for the provided ASN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/as_stats/15169)
GET |  /db_rest/v1/asn_stats/ipv6 | IPv4  **limit** - Max results to return<br> **topTransit=<number>** Get the top transit prefixes<br> **topOrigin=<number>** Get the top originating ASN's | [Demo](http://demo.openbmp.org:8001/db_rest/v1/as_stats/ipv4?topOrigin=25)
GET |  /db_rest/v1/asn_stats/ipv6 | IPv6  **limit** - Max results to return<br> **topTransit=<number>** Get the top transit prefixes<br> **topOrigin=<number>** Get the top originating ASN's | [Demo](http://demo.openbmp.org:8001/db_rest/v1/as_stats/ipv6?topTransit=25)




### Whois ASN Information
Whois information for ASN's

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/whois/asn | **where** - SQL WHERE clause | Returns all whois information - **WARNING** will be a lot of data. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/whois/asn)
GET | /db_rest/v1/whois/asn/{asn} |  **where** - SQL WHERE clause  | Returns the whois info for the given ASN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/whois/asn/16509)

Build
-----
Source is available in [Github](https://github.com/OpenBMP/db_rest)

To build the WAR file, run **mvn clean package**
> You have to have maven 3 installed.  To install maven see [maven3](http://maven.apache.org/download.cgi)

Install
-------
To install, simply copy the **db_rest.war** file to the **\<*tomcat*\>/webapps** directory. 



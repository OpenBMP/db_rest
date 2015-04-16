DB_REST - OpenBMP Database REST Service
---------------------------------------

Simple DB REST interface to the OpenBMP database. This is an example of how to interact with the database. 

<div style="color:blue; font-size:32px">Demo server is still in development.</div>


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
GET | /db_rest/v1/routers | **withgeo** - Adds the geo location columns for router based on the router IP address<br> **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns the list of BMP routers currently known in the database | [Demo](http://demo.openbmp.org:8001/db_rest/v1/routers)
GET | /db_rest/v1/routers/status/count | | Returns a count of DOWN and UP BMP routers | [Demo](http://demo.openbmp.org:8001/db_rest/v1/routers/status/count)
GET | /db_rest/v1/routers/status/up | | Returns the BMP routers that are up | [Demo](http://demo.openbmp.org:8001/db_rest/v1/routers/status/up)
GET | /db_rest/v1/routers/status/down | | Returns the BMP routers that are down | [Demo](http://demo.openbmp.org:8001/db_rest/v1/routers/status/down)

### BGP Peers
BGP Peer view. 

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/peer | **withgeo** - Adds the geo location columns for the peer based on the peer IP address<br> **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of BGP peers | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer)
GET | /db_rest/v1/peer/localip/{localIP} | **withgeo** - Adds the geo location columns for the peer based on the peer IP address<br> **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of BGP peers by local IP address | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/localip/10.22.165.141)
GET | /db_rest/v1/peer/remoteip/{remoteIP} | **withgeo** - Adds the geo location columns for the peer based on the peer IP address<br> **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of BGP peers by remote/peer IP address | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/remoteip/172.20.164.43)
GET | /db_rest/v1/peer/asn/{Peer ASN} | **withgeo** - Adds the geo location columns for the peer based on the peer IP address<br> **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of BGP peers by Peer ASN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/asn/64555)
GET | /db_rest/v1/peer/type/count | | Returns peer counts by status IP version type | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/type/count)
GET | /db_rest/v1/peer/type/count/router | | Returns a list of routers with count of peers by IP Type| [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/type/count/router)
GET | /db_rest/v1/peer/type/v4 | **withgeo** - Adds the geo location columns for the peer based on the peer IP address<br> | Returns list of IPv4 peers  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/type/v4)
GET | /db_rest/v1/peer/type/v6 | **withgeo** - Adds the geo location columns for the peer based on the peer IP address<br> | Returns list of IPv6 peers  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/type/v6)
GET | /db_rest/v1/peer/status/count | | Returns peer counts by peer status  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/status/count)
GET | /db_rest/v1/peer/status/up | **withgeo** - Adds the geo location columns for the peer based on the peer IP address<br> | Returns a list of BGP peers that are UP | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/status/up)
GET | /db_rest/v1/peer/status/down | **withgeo** - Adds the geo location columns for the peer based on the peer IP address<br> | Returns a list of BGP peers that are DOWN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/status/down)
GET | /db_rest/v1/peer/prefix | | Returns a list of BGP peers and their Pre-Policy and Post-Policy RIB counts. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/prefix)
GET | /db_rest/v1/peer/prefix/{peerIP} | | Returns a list of BGP peers and their Pre-Policy and Post-Policy RIB counts by peer address | [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/prefix/172.20.164.43)
GET | /db_rest/v1/peer/router/{Router IP} | **withgeo** - Adds the geo location columns for the peer based on the peer IP address<br> **limit** - Max results to return<br>**orderby** - SQL ORDER BY clause | Returns a list of BGP peers based on Router IP or Router Name| [Demo](http://demo.openbmp.org:8001/db_rest/v1/peer/router/10.22.165.140)




### RIB
Unicast IPv4/IPv6 RIB/routes view. 

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/rib |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns the RIB entries.  Default limit is 1000.  Should use WHERE clause to filter or use one of the below URI's instead.| [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib?where=PrefixLen >= 8 and PrefixLen <= 9)
GET | /db_rest/v1/rib/asn/{ASN} |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of prefixes by Origin ASN.  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/asn/16509)
GET | /db_rest/v1/rib/asn/{ASN}/count |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of prefixes by Origin ASN.  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/asn/16509/count)
GET | /db_rest/v1/rib/prefix/{Prefix} |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of prefixes by Prefix starts with.  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/prefix/216.40)
GET | /db_rest/v1/rib/prefix/{Prefix}/{length} |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Returns a list of prefixes by Prefix/Length.  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/prefix/216.40.0.0/20)
GET | /db_rest/v1/rib/lookup/{IP} | **where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause | Finds the longest match prefix based on the IP (more specific) supplied.  | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/lookup/173.39.211.144)
GET | /db_rest/v1/rib/history/{prefix}/{length} |  **limit** - Max results to return<br>**where** - SQL WHERE clause<br> **orderby** - SQL ORDER BY clause <br>**days** number of days to include in search.| Returns the RIB history for a given prefix/length.  Default limit is 1000.  Can use WHERE clause to filter on PeerName to restrict to a specific peer. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/rib/history/176.105.176.0/24)

### Upstream BGP ASN's (peers)
List the upstream ASN's for the given ASN.  This query simply looks at the AS PATH and parses out the LEFT most ASN next to the ASN provided.

> ### For example
> AS PATH 1 = 100 200 **300** *400* 500 600 700<br>
> AS PATH 2 = 40 50 60 **70** *400* 888 600 700<br>
> A query on ASN = **400** returns { **300**, **70** } since both of those ASN's are the left most ASN's next to ASN 400.

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET |  /db_rest/v1/upstream/{asn} |  | List all upstream ASN's for the given ASN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/upstream/16509)
GET |  /db_rest/v1/upstream/{asn}/count |  | List all upstream ASN's for the given ASN and count the number of distinct prefixes (each prefix/len + peer is counted) | [Demo](http://demo.openbmp.org:8001/db_rest/v1/upstream/16509/count)
GET |  /db_rest/v1/upstream/{asn}/peer/count |  | List all upstream ASN's for the given ASN and count the number of distinct prefixes per peer | [Demo](http://demo.openbmp.org:8001/db_rest/v1/upstream/16509/peer/count)

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
GET |  /db_rest/v1/downstream/peer/{peerHashId} | | Distinctly lists the ASN's that are directly to the right of the peer ASN. E.g. ASPath's { "10 20, 30", "10 9 8"}  would produce a list containing 20 and 9 since those ASNs are to the right of the peering ASN of 10.   Each AS will have the ASN name, organization, and country.   | [Demo](http://demo.openbmp.org:8001/db_rest/v1/downstream/peer/c33f36c12036e98d89ae3ea54cce0be2)

### Updates Over Time
Each update is logged.  Attributes that vary by router/peer/prefix will get an update log entry.

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/updates/top | **limit** - Max results to return<br> **hours** - Number of hours to include in top | Return the top prefixes by number of updates in past 'hours'.   Limit defaults to 25 and hours defaults to 2. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/updates/top?limit=15)
GET | /db_rest/v1/updates/peer/{peerHashId}/top | **limit** - Max results to return<br> **hours** - Number of hours to include in top | For given peer, return the top prefixes by number of updates in past 'hours'.   Limit defaults to 25 and hours defaults to 2. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/updates/peer/c33f36c12036e98d89ae3ea54cce0be2/top?limit=15)
GET | /db_rest/v1/updates/top/interval/{minutes} | **minutes** - Interval in minutes<br> **limit** - Max results to return<br> **hours** - Number of hours to include in top | Return the count of updates per interval for up to max limit size. Limit defaults to 25. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/updates/top/interval/5)
GET | /db_rest/v1/updates/peer/{peerHashId}/top/interval/{minutes} | **minutes** - Interval in minutes<br> **limit** - Max results to return<br> **hours** - Number of hours to include in top | For given peer, return the count of updates per interval for up to max limit size. Limit defaults to 25. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/updates/peer/c33f36c12036e98d89ae3ea54cce0be2/top/interval/5)



### Withdrawns Over Time
Each prefix withdrawn is logged.  

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/withdrawns/top | **limit** - Max results to return<br> **hours** - Number of hours to include in top | Return the top prefixes by number of prefixes withdrawn in past 'hours'.   Limit defaults to 25 and hours defaults to 2. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/withdrawns/top?limit=15)
GET | /db_rest/v1/withdrawns/peer/{peerHashId}/top | **limit** - Max results to return<br> **hours** - Number of hours to include in top | For given peer, return the top prefixes by number of prefixes withdrawn in past 'hours'.   Limit defaults to 25 and hours defaults to 2. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/withdrawns/peer/c33f36c12036e98d89ae3ea54cce0be2/top?limit=15)
GET | /db_rest/v1/withdrawns/top/interval/{minutes} | **minutes** - Interval in minutes<br> **limit** - Max results to return<br> **hours** - Number of hours to include in top | Return the count of updates per interval for up to max limit size. Limit defaults to 25. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/withdrawns/top/interval/5)
GET | /db_rest/v1/withdrawns/peer/{peerHashId}/top/interval/{minutes} | **minutes** - Interval in minutes<br> **limit** - Max results to return<br> **hours** - Number of hours to include in top | Return the count of updates per interval for up to max limit size. Limit defaults to 25. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/withdrawns/peer/c33f36c12036e98d89ae3ea54cce0be2/top/interval/5)

### AS Stastistics
Per ASN statistics

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET |  /db_rest/v1/asn_stats |  **limit** - Max results to return<br> | Returns a list of all active ASN's in the DB | [Demo](http://demo.openbmp.org:8001/db_rest/v1/as_stats?limit=100)
GET | /db_rest/v1/asn_stats/{asn} |  **limit** - Max results to return<br> | Returns the AS statistics for the provided ASN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/as_stats/15169)
GET |  /db_rest/v1/asn_stats/ipv4 | IPv4  **limit** - Max results to return<br> **topTransit=<number>** Get the top transit prefixes<br> **topOrigin=<number>** Get the top originating ASN's | Get the top ASN's originating prefixes or that are transit to other ASN's | [Demo](http://demo.openbmp.org:8001/db_rest/v1/as_stats/ipv4?topOrigin=25)
GET |  /db_rest/v1/asn_stats/ipv6 | IPv6  **limit** - Max results to return<br> **topTransit=<number>** Get the top transit prefixes<br> **topOrigin=<number>** Get the top originating ASN's | Get the top ASN's originating prefixes or that are transit to other ASN's | [Demo](http://demo.openbmp.org:8001/db_rest/v1/as_stats/ipv6?topTransit=25)




### Whois ASN Information
Whois information for ASN's

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/whois/asn | **where** - SQL WHERE clause<p>**limit** - Max results to return | Returns all whois information | [Demo](http://demo.openbmp.org:8001/db_rest/v1/whois/asn?where=org_name like%20'%cisco%')
GET | /db_rest/v1/whois/asn/count | **where** - SQL WHERE clause<p> | Returns the count of ASN matches based on WHERE clause | [Demo](http://demo.openbmp.org:8001/db_rest/v1/whois/asn/count?where=org_name like%20'%cisco%')
GET | /db_rest/v1/whois/asn/{asn} |  **where** - SQL WHERE clause  | Returns the whois info for the given ASN | [Demo](http://demo.openbmp.org:8001/db_rest/v1/whois/asn/16509)

### Geolocation IP Information
Geolocation IP address information from DB-IP, IP2Location, or MaxMind. 

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/geoip/{IP} | **where** - SQL WHERE clause| Returns the geolocation data based on the IP supplied | [Demo](http://demo.openbmp.org:8001/db_rest/v1/geoip/72.163.4.161)

### BGP-LS Information
BGP-LS general/raw link state DB information

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/linkstate/nodes | **where** - SQL WHERE clause<br> **limit** - Limit the number of results<br> **orderby** - Order by clause| Returns the list of link state nodes | [Demo](http://demo.openbmp.org:8001/db_rest/v1/linkstate/nodes)
GET | /db_rest/v1/linkstate/nodes/peer/{peerHashId} | **where** - SQL WHERE clause<br> **limit** - Limit the number of results<br> **orderby** - Order by clause| Returns the list of link state nodes for the given **peer hash id** </p>*Get the peer hash ID by listing the nodes.*| [Demo](http://demo.openbmp.org:8001/db_rest/v1/linkstate/nodes/peer/daaa681792b33e36166a2205be05868d)
GET | /db_rest/v1/linkstate/nodes/peer/{peerHashId}/{nodeHashId} | **where** - SQL WHERE clause<br> **limit** - Limit the number of results<br> **orderby** - Order by clause| Returns the the specific node by **peer hash id** and **node hash id**  </p>*Get the node and peer hash ID by listing the nodes.*| [Demo](http://demo.openbmp.org:8001/db_rest/v1/linkstate/nodes/peer/daaa681792b33e36166a2205be05868d/08625b6f7dea7ecc5a559b77afdb458a)
GET | /db_rest/v1/linkstate/links | **where** - SQL WHERE clause<br> **limit** - Limit the number of results<br> **orderby** - Order by clause| Returns the list of link state links | [Demo](http://demo.openbmp.org:8001/db_rest/v1/linkstate/links)
GET | /db_rest/v1/linkstate/links/peer/{peerHashId} | **where** - SQL WHERE clause<br> **limit** - Limit the number of results<br> **orderby** - Order by clause| Returns the list of link state links for given **peer hash id**| [Demo](http://demo.openbmp.org:8001/db_rest/v1/linkstate/links/peer/daaa681792b33e36166a2205be05868d)
GET | /db_rest/v1/linkstate/prefixes | **where** - SQL WHERE clause<br> **limit** - Limit the number of results<br> **orderby** - Order by clause| Returns the list of link state prefixes | [Demo](http://demo.openbmp.org:8001/db_rest/v1/linkstate/prefixes)
GET | /db_rest/v1/linkstate/prefixes/peer/{peerHashId} | **where** - SQL WHERE clause<br> **limit** - Limit the number of results<br> **orderby** - Order by clause| Returns the list of link state prefixes for given **peer hash id**| [Demo](http://demo.openbmp.org:8001/db_rest/v1/linkstate/prefixes/peer/daaa681792b33e36166a2205be05868d)

### BGP-LS SPF Information
BGP-LS shortest path first (SPF) information.  

Running any GET below will result in a SPF generated routing table rooted at the given 
router id.  IS-IS will have a router ID as well, so we use router id regardless of
OSPF or IS-IS.   This keeps it consistent between the two. 

In order to ensure that fast/repeated queries do not result in running SPF's when there's no change, there is a built in timer that will only generate a new RIB for the given protocol and router ID at most every 15 seconds.

> SPF's are calculated on a per-BGP-peer basis.  A single BGP peer provides all link state
> information so the SPF does not need to span multiple bgp-ls peers.  You should first run a
> **GET /db_rest/v1/linkstate/nodes** to identify which peer and node you would like
> to run the SPF against.  

Method | URI       | Parameters    | Description | Demo URL                      
------ | ----------| ------------- | ----------- | --------
GET | /db_rest/v1/linkstate/spf/peer/{peerHashId}/ospf/{routerId} | **where** - SQL WHERE clause<br> **limit** - Limit the number of results<br> **orderby** - Order by clause| Runs **OSPF** SPF and generates a RIB for the given bgp-ls peer and router-id.  The generated RIB is returned. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/linkstate/spf/peer/54ecaeeec115457cbce466ff48857aa7/ospf/100.1.1.2)
GET | /db_rest/v1/linkstate/spf/peer/{peerHashId}/isis/{routerId} | **where** - SQL WHERE clause<br> **limit** - Limit the number of results<br> **orderby** - Order by clause| Runs **IS-IS** SPF and generates a RIB for the given bgp-ls peer and router-id.  The generated RIB is returned. | [Demo](http://demo.openbmp.org:8001/db_rest/v1/linkstate/spf/peer/daaa681792b33e36166a2205be05868d/isis/200.1.1.1)

#### Drawing the topology
The returned result will contain two path columns/fields.  

* **path_hash_ids** - Comma delimited list of node hash ID's  
* **path_router_ids** - Comma delimited list of node router ID's

The **first entry** in the path will always be the **root node**.  For example, if the SPF was ran for 200.1.1.1, then 200.1.1.1 will be the first path entry.  The **last entry** in the path will always be the **node** where the prefix **originates**.  

```
{
   prefix: "100.1.1.4/32",
   Type: "0",
   metric: 21,
   src_router_id: "100.1.1.4",
   nei_router_id: "100.1.1.3",
   path_router_ids: "100.1.1.2,100.1.1.3,100.1.1.4",
   path_hash_ids: "0ec92251bd07fc3a917ace4fcc564c83,d9ea9b11a9c9e2a9e91acfb91711f4a8,af17bf43842d5bac5702c5971334cce0",
   neighbor_addr: "192.168.1.10",
   peer_hash_id: "54ecaeeec115457cbce466ff48857aa7"
}
```

Build
-----
Source is available in [Github](https://github.com/OpenBMP/db_rest)

Before building, edit the **src/main/webapp/META-INF/context.xml** file and change the settings to match your DB install (username, password, and hostname). 

To build the WAR file, run **mvn clean package**
> You have to have maven version 3 installed.  To install maven see [maven3](http://maven.apache.org/download.cgi)

Install
-------
To install, simply copy the **db_rest.war** file to the **\<*tomcat*\>/webapps** directory. 



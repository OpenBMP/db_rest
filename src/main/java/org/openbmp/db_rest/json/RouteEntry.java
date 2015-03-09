/*
 * Copyright (c) 2014-2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest.json;

public class RouteEntry {
    private String RouterName;
    private String PeerName;
    private String Prefix;
    private Integer PrefixLen;
    private String Origin;
    private Integer OriginAS;
    private Integer Med;
    private Integer LocalPref;
    private String NextHop;
    private String AsPath;
    private Integer AsPathCount;
    private String Communities;
    private String ClusterList;
    private String Aggregator;
    private String PeerAddress;
    private Integer PeerAsn;
    private String LastModified;


    public RouteEntry setRouterName(String value) { RouterName = value; return this; }
    public RouteEntry setPeerName(String value) { PeerName = value; return this; }
    public RouteEntry setPrefix(String value) { Prefix = value; return this; }
    public RouteEntry setOrigin(String value) { Origin = value; return this; }
    public RouteEntry setNextHop(String value) { NextHop = value; return this; }
    public RouteEntry setAsPath(String value) { AsPath = value; return this; }
    public RouteEntry setCommunities(String value) { Communities = value; return this; }
    public RouteEntry setClusterList(String value) { ClusterList = value; return this; }
    public RouteEntry setAggregator(String value) { Aggregator = value; return this; }
    public RouteEntry setPeerAddress(String value) { PeerAddress = value; return this; }
    public RouteEntry setLastModified(String value) { LastModified = value; return this; }
    
    public RouteEntry setPrefixLen(Integer value) { PrefixLen = value; return this; }
    public RouteEntry setOriginAS(Integer value) { OriginAS = value; return this; }
    public RouteEntry setMed(Integer value) { Med = value; return this; }
    public RouteEntry setLocalPref(Integer value) { LocalPref = value; return this; }
    public RouteEntry setAsPathCount(Integer value) { AsPathCount = value; return this; }
    public RouteEntry setPeerAsn(Integer value) { PeerAsn = value; return this; }
    
    public String getRouterName() { return RouterName; }
    public String getPeerName() { return PeerName; }
    public String getPeerPrefix() { return Prefix; }
    public String getOrigin() { return Origin; }
    public String getNextHop() { return NextHop; }
    public String getAsPath() { return AsPath; }
    public String getCommunities() { return Communities; }
    public String getClusterList() { return ClusterList; }
    public String getAggregator() { return Aggregator; }
    public String getPeerAddress() { return PeerAddress; }
    public String getLastModified() { return LastModified; }
    
    public Integer getPrefixLen() { return PrefixLen; }
    public Integer getOriginAS() { return OriginAS; }
    public Integer getMed() { return Med; }
    public Integer getLocalPref() { return LocalPref; }
    public Integer getAsPathCount() { return AsPathCount; }
    public Integer getPeerAsn() { return PeerAsn; }
}

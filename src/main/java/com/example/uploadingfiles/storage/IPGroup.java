package com.example.uploadingfiles.storage;

import org.json.simple.JSONArray;

import java.util.HashSet;
import java.util.Set;

public class IPGroup {
    private String name;
    private Set<String> ips = new HashSet<>();

    public IPGroup(String name, JSONArray ipAddresses) {
        this.name = name;
        ips.addAll(ipAddresses);
    }

    public String getName() {
        return name;
    }

    public Set<String> getIps() {
        return ips;
    }
}

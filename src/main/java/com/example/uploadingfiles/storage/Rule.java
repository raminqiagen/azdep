package com.example.uploadingfiles.storage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Rule {
    String name;
    Set<String> sourceAddresses = new HashSet<>();
    Set<String> sourceIpGroups = new HashSet<>();
    Set<String> destinationFqdns = new HashSet<>();
    Set<String> destinationPorts = new HashSet<>();
    Set<String> destinationAddresses = new HashSet<>();
    Set<String> destinationIpGroups = new HashSet<>();
    Set<String> protocols = new HashSet<>();

    public Rule(JSONObject rule) {
        name = (String) rule.get("name");
        sourceAddresses.addAll((JSONArray)rule.get("sourceAddresses"));
        ((JSONArray)rule.get("sourceIpGroups")).stream().map(s -> ((String) s).replaceAll("\\[resourceId\\('Microsoft.Network/ipGroups', ", "")).map(s -> ((String) s).replaceAll("\\)\\]", "")).forEach(s -> sourceIpGroups.add((String) s));
        destinationFqdns.addAll((JSONArray)rule.get("destinationFqdns"));
        destinationPorts.addAll((JSONArray)rule.get("destinationPorts"));
        destinationAddresses.addAll((JSONArray)rule.get("destinationAddresses"));
        ((JSONArray)rule.get("destinationIpGroups")).stream().map(s -> ((String) s).replaceAll("\\[resourceId\\('Microsoft.Network/ipGroups', ", "")).map(s -> ((String) s).replaceAll("\\)\\]", "")).forEach(s -> destinationIpGroups.add((String) s));
        protocols.addAll((JSONArray)rule.get("protocols"));
    }

    public boolean match(String name, String sourceIP, String destinationIP, String port, Map<String, IPGroup> ipGroups) {
        if (protocols.contains("ICMP")) return false;//TODO
        if (destinationPorts.contains("*") || destinationPorts.contains(port) ||
                destinationPorts.stream().filter(p -> p.contains("-")).anyMatch(p -> {
                    int pstart = Integer.parseInt(p.substring(0, p.indexOf('-')));
                    int pend = Integer.parseInt(p.substring(p.indexOf('-') + 1));
                    int pint = Integer.parseInt(port);
                    return pint >= pstart && pint <= pend;
                })
        ) {
//            System.out.println("port match: " + port + ":" + destinationPorts);
            if (sourceAddresses.contains("*") || sourceAddresses.contains(sourceIP) || sourceIpGroups.stream().anyMatch(g -> ipGroupMatch(g, sourceIP, ipGroups))) {
//                System.out.println("source ip match: " + sourceIP + ":" + sourceAddresses + " / " + sourceIpGroups);
                if (destinationAddresses.contains("*") || destinationAddresses.contains(destinationIP) || destinationIpGroups.stream().anyMatch(g -> ipGroupMatch(g, destinationIP, ipGroups))) {
//                    System.out.println("dest ip match: " + destinationIP + ":" + destinationAddresses + " / " + destinationIpGroups);
                    System.out.println("rule match[" + name + "]: " + sourceIP + " => " + destinationIP + ":" + port);
                    return true;
                } else {
//                    System.out.println("dest ip mismatch: " + destinationIP + ":" + destinationAddresses + " / " + destinationIpGroups);
                }
            } else {
//                System.out.println("source ip mismatch: " + sourceIP + ":" + sourceAddresses + " / " + sourceIpGroups);
            }
        } else {
//            System.out.println("port mismatch: " + port + ":" + destinationPorts);
        }
//        System.out.println("no match: " + sourceIP + " -> " + destinationIP + ":" + port);
        return false;
    }

    private boolean ipGroupMatch(String ipGroupName, String ip, Map<String, IPGroup> ipGroups) {
        Set<String> ips = ipGroups.get(ipGroupName).getIps();
        for (String subnet : ips) {
            if (subnet.contains("/")) {
                IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(subnet);
                if (ipAddressMatcher.matches(ip)) return true;
            } else {
                if (subnet.equals(ip)) return true;
            }
        }
        return false;
    }

}

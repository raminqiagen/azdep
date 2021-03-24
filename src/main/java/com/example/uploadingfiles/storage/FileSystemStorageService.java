package com.example.uploadingfiles.storage;

import com.opencsv.CSVReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.*;

@Service
public class FileSystemStorageService implements StorageService {

	private List<String> result = new ArrayList<>();

	@Override
	public void process(MultipartFile fw, MultipartFile dep, String oldIP, String newIP) {
		result = new ArrayList<>();

		Map<String, IPGroup> ipGroups = new HashMap<>();
		List<Rule> rules = new ArrayList<>();
		JSONParser parser = new JSONParser();
		try {
			JSONObject jsonObject = (JSONObject) parser.parse(new InputStreamReader(fw.getInputStream()));

			JSONArray resources = (JSONArray) jsonObject.get("resources");
			for (Object o : resources) {
				JSONObject resource = (JSONObject) o;
				String name = (String) resource.get("name");
				String type = (String) resource.get("type");
				JSONObject properties = (JSONObject) resource.get("properties");
				if (type.equals("Microsoft.Network/ipGroups")) {
					JSONArray ipAddresses = (JSONArray) properties.get("ipAddresses");
					name = name.replaceAll("\\[", "").replaceAll("\\]", "");
					IPGroup ipGroup = new IPGroup(name, ipAddresses);
					ipGroups.put(name, ipGroup);
				} else if (type.equals("Microsoft.Network/azureFirewalls")) {
					JSONArray networkRuleCollections = (JSONArray) properties.get("networkRuleCollections");
					for (Object ruleCollection : networkRuleCollections) {
						JSONObject networkRuleCollection = (JSONObject) ruleCollection;
						String colName = (String) networkRuleCollection.get("name");
						JSONObject colProperties = (JSONObject) networkRuleCollection.get("properties");
						String action = (String) ((JSONObject) colProperties.get("action")).get("type");
						long priority = (long) colProperties.get("priority");
						JSONArray rulesArray = (JSONArray) colProperties.get("rules");
						rulesArray.forEach(r -> rules.add(new Rule((JSONObject) r)));
					}
				}
			}
			String rulesCount = "#rules = " + rules.size();
			System.out.println(rulesCount);
//			result.add(rulesCount);

			try (CSVReader csvreader = new CSVReader(new InputStreamReader(dep.getInputStream()))) {
				List<String[]> r = csvreader.readAll();

				Set<String> nonMatches = new HashSet<>();

				r.stream().filter(s -> {
					String sourceIP = s[2];
					String destinationIP = s[6];
					return sourceIP.equals(oldIP) || destinationIP.equals(oldIP);
				}).forEach(s -> {
					String sourceIP = s[2].equals(oldIP) ? newIP : s[2];
					String destinationIP = s[6].equals(oldIP) ? newIP : s[6];
					String port = s[9];
					boolean result = rules.stream().anyMatch(rule -> rule.match(rule.name, sourceIP, destinationIP, port, ipGroups));
					if (!result) {
						String nonMatch = sourceIP + " => " + destinationIP + ":" + port;
						nonMatches.add(nonMatch);
					}
				});

				System.out.println("====================Not matched with any firewall rule => will be blocked ======================");
//				nonMatches.forEach(System.out::println);
				result.addAll(nonMatches);
			}
			System.out.println(getResult());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public List<String> getResult() {
		return result;
	}
}

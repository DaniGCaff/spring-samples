package com.danigcaff.springframework.samples.spring_rest_listener.repoHooksListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.danigcaff.springframework.samples.spring_web.util.MongoManager;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

@RestController
public class GitHubHookListener {
	private static String TYPE = "event_type";
	private static String PUSH_TYPE = "pusher";
	private static String MEMBER_TYPE = "member";

	public JSONObject eventClasification(JSONObject json) {
		if (json.has(PUSH_TYPE))
			json.put(TYPE, PUSH_TYPE);
		else if (json.has(MEMBER_TYPE))
			json.put(TYPE, MEMBER_TYPE);
		return json;
	}

	@RequestMapping(value = "/listen/repos/github", method = RequestMethod.POST)
	public void repoListener(@RequestBody String evento) {
		System.out.println(evento);
		JSONObject json = new JSONObject(evento);
		json = eventClasification(json);
		DB database = MongoManager.getManager().getDatabase();
		DBObject doc = (DBObject) (JSON.parse(json.toString()));
		if (!database.collectionExists((String) json.get(TYPE))) {
			DBObject options = BasicDBObjectBuilder.start().add("capped", false).get();
			database.createCollection((String) json.get(TYPE), options);
		}
		DBCollection coll = database.getCollection((String) json.get(TYPE));
		coll.insert(doc);

		if (json.has(PUSH_TYPE)) {
			ArrayList<Map<String, String>> mapCommitTask = getDataFromCommit(json);
			Iterator<Map<String, String>> iterador = mapCommitTask.iterator();
			while (iterador.hasNext()) {
				Map<String, String> asocCommitTask = iterador.next();
				String commit =  (String)new JSONObject((String)asocCommitTask.get("commit")).get("message");
				BasicDBObject query = new BasicDBObject("shortUrl", asocCommitTask.get("urlTask"));
				DBCollection collTask = database.getCollection("TASKS");
				DBObject docResult = collTask.findOne(query);
				if (docResult != null) {
					String idTarjeta = docResult.get("id").toString();
					if (!database.collectionExists("TASKS")) {
						DBObject options = BasicDBObjectBuilder.start().add("capped", false).get();
						database.createCollection("TASKS", options);
					}
					DBCollection collAsociado = database.getCollection("TASKS");
					DBObject updateDoc = new BasicDBObject();
					updateDoc.put("$push", new BasicDBObject("commits", (DBObject) (JSON.parse(asocCommitTask.get("commit")))));
					DBObject filter = new BasicDBObject("id", idTarjeta);
					collAsociado.update(filter, updateDoc);
				} else
					System.out.println("No hay resultados...");
			}

		}
	}

	private ArrayList<Map<String, String>> getDataFromCommit(JSONObject json) {
		ArrayList<Map<String, String>> mapUrlIds = new ArrayList<Map<String, String>>();
		String url;
		String message;
		JSONArray commitsArray = (JSONArray) json.get("commits");
		for (int i = 0; i < commitsArray.length(); i++) {
			JSONObject rec = commitsArray.getJSONObject(i);
			message = rec.getString("message");
			Pattern pattern = Pattern.compile("@https://trello.com/c/\\w+@");
			Matcher matcher = pattern.matcher(message);
			while (matcher.find()) {
				Map<String, String> map = new HashMap<String, String>();
				url = matcher.group().substring(1, matcher.group().length() - 1);
				map.put("commit", rec.toString());
				map.put("urlTask", url);
				mapUrlIds.add(map);
			}
		}
		return mapUrlIds;
	}
}

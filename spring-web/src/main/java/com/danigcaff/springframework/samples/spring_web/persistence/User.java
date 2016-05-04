package com.danigcaff.springframework.samples.spring_web.persistence;

public interface User extends Entity {
	public String getName();
	public User setName(String name);
	public String getGitHubUserId();
	public User setGitHubUser(String gitHubUserId);
	public String getTrelloUserId();
	public User setTrelloUser(String trelloUserId);
}

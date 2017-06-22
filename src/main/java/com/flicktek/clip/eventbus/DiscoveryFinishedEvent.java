package com.flicktek.clip.eventbus;

public class DiscoveryFinishedEvent {
	public final Boolean found;

	public DiscoveryFinishedEvent(Boolean found) {
		this.found = found;
	}
}

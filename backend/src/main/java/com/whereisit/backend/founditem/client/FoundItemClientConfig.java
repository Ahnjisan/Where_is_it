package com.whereisit.backend.founditem.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.whereisit.backend.founditem.entity.FoundItemSourceType;

@Configuration
@EnableConfigurationProperties(FoundItemApiProperties.class)
public class FoundItemClientConfig {

	@Bean
	public FoundItemLookupClient policeFoundItemLookupClient(RestClient.Builder builder, FoundItemApiProperties properties) {
		return new FoundItemLookupClient(builder,
				properties.getPoliceFoundItemUrl(), properties.getServiceKey(), "FD_COL_CD", FoundItemSourceType.POLICE);
	}

	@Bean
	public FoundItemLookupClient portalFoundItemLookupClient(RestClient.Builder builder, FoundItemApiProperties properties) {
		return new FoundItemLookupClient(builder,
				properties.getPortalFoundItemUrl(), properties.getServiceKey(), "CLR_CD", FoundItemSourceType.PORTAL);
	}

	@Bean
	public PortalFoundItemNameStorageClient portalFoundItemNameStorageClient(
			RestClient.Builder builder, FoundItemApiProperties properties) {
		FoundItemLookupClient delegate = new FoundItemLookupClient(builder,
				properties.getPortalNameStorageFoundItemUrl(), properties.getServiceKey(),
				"CLR_CD", FoundItemSourceType.PORTAL);
		return new PortalFoundItemNameStorageClient(delegate);
	public FoundItemDetailClient policeFoundItemDetailClient(RestClient.Builder builder, FoundItemApiProperties properties) {
		return new FoundItemDetailClient(builder,
				properties.getPoliceFoundItemDetailUrl(), properties.getServiceKey(), FoundItemSourceType.POLICE);
	}

	@Bean
	public FoundItemDetailClient portalFoundItemDetailClient(RestClient.Builder builder, FoundItemApiProperties properties) {
		return new FoundItemDetailClient(builder,
				properties.getPortalFoundItemDetailUrl(), properties.getServiceKey(), FoundItemSourceType.PORTAL);
	}
}

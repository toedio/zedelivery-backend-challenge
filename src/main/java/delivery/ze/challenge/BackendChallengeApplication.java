package delivery.ze.challenge;

import com.fasterxml.jackson.databind.ObjectMapper;
import delivery.ze.challenge.domain.Partner;
import delivery.ze.challenge.dto.PartnerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.repository.init.Jackson2RepositoryPopulatorFactoryBean;

@SpringBootApplication
@EnableCaching
public class BackendChallengeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendChallengeApplication.class, args);
	}

	@Value("classpath:data.json")
	private Resource initialData;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Bean
	@Primary
	public RedisCacheConfiguration defaultCacheConfig(ObjectMapper objectMapper) {
		Jackson2JsonRedisSerializer<PartnerDTO> serializer = new Jackson2JsonRedisSerializer<>(PartnerDTO.class);
		serializer.setObjectMapper(objectMapper);
		return RedisCacheConfiguration.defaultCacheConfig()
				.serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(SerializationPair.fromSerializer(serializer))
				.prefixKeysWith("");
	}

	/*
	TODO
	    - Delete and do in docker
	 */
	@Bean
	@Profile({"!prd"})
	public Jackson2RepositoryPopulatorFactoryBean loadInitial(ObjectMapper objectMapper) {
		Jackson2RepositoryPopulatorFactoryBean factory = new Jackson2RepositoryPopulatorFactoryBean();
		factory.setMapper(objectMapper);
		factory.setResources(new Resource[]{initialData});
		return factory;
	}
	/*
	TODO
		- Delete and do in docker
	 */

	@EventListener(ApplicationReadyEvent.class)
	public void initIndicesAfterStartup() {
		IndexOperations indexOperations = mongoTemplate.indexOps(Partner.class);
		indexOperations.ensureIndex(new GeospatialIndex("address").typed(GeoSpatialIndexType.GEO_2DSPHERE));
		indexOperations.ensureIndex(new Index("document", Sort.Direction.ASC).unique());
	}
}
